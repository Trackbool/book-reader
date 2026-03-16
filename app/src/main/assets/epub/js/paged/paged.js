// ─────────────────────────────────────────────────────────────────────────────
// EPUB Paged Reader — iframe edition
//
// Each chapter renders in its own <iframe>. While narrow (colWidth), CSS
// columns overflow rightward in layout space, letting Range measure the total
// width precisely. The iframe is then widened to pageCount × colWidth so every
// column sits within its rendered viewport. The parent slides a container div
// to reveal individual pages.
//
// Sub-pixel precision:
//   Page count is derived from Range.getBoundingClientRect() on the narrow
//   iframe. colWidth = Math.floor(contentEl.width) for consistent integer CSS
//   pixels that match the Range measurement.
//
// Cross-chapter pagination:
//   Chapters are laid out consecutively. Global page N maps to the chapter
//   where chapter.startPage ≤ N < nextChapter.startPage. A translateX of
//   −N × colWidth on the container exposes the correct column from whichever
//   iframe owns that page.
//
// Touch handling:
//   A transparent overlay div in the parent document captures all touch events
//   directly — no cross-iframe forwarding. This mirrors the Shadow DOM version
//   and gives the same smooth drag performance.
//
// Progress format:
//   { chapterId: string, nodeIndex: number, nodeOffset: number [0, 1) }
// ─────────────────────────────────────────────────────────────────────────────

// BLOCK_SELECTOR is declared in epub_base.js.

// ─── Constants ────────────────────────────────────────────────────────────────

/** Minimum swipe distance as a fraction of colWidth to trigger a page turn. */
const SWIPE_THRESHOLD = 0.20;

/** Minimum swipe velocity (px/ms) to trigger a page turn regardless of distance. */
const SWIPE_VELOCITY = 0.30;

/** CSS transition used for page navigation animations. */
const TRANSITION_PAGE = 'transform .3s ease';

/** Duration of the page transition in ms, must match TRANSITION_PAGE. */
const TRANSITION_DURATION = 300;

// ─── State ────────────────────────────────────────────────────────────────────

/** Outer overflow-hidden viewport container. */
let contentEl;

/** Full-width strip translated left/right to expose individual pages. */
let chapterContainer;

/**
 * Ordered list of loaded chapter descriptors.
 * @type {Array<{
 *   id: string,
 *   el: HTMLIFrameElement,
 *   pageCount: number,
 *   startPage: number,
 *   nodes: Array<{ el: Element, startPage: number, pageCount: number, endPage: number }>
 * }>}
 */
let chapters = [];

let currentPage = 0;
let totalPages  = 0;

/** CSS-pixel width of one column / page. Always equals Math.floor(contentEl.width). */
let colWidth = 0;

/** Last emitted progress snapshot, used to restore position after resize. */
let lastProgress = null;

/** Guards onResize against running before the initial content load completes. */
let _contentReady = false;



// ─── Initialisation ──────────────────────────────────────────────────────────

function init() {
    contentEl        = document.getElementById('content');
    chapterContainer = document.getElementById('chapter-container');
    colWidth         = _readColumnWidth();
    _initSwipeGesture();
    setupTapDetector(); // epub_base.js — attaches to the parent document
}

document.addEventListener('DOMContentLoaded', init);

// ─── Public API ───────────────────────────────────────────────────────────────

/**
 * Loads all chapters, measures their page counts, and navigates to the saved
 * progress position (or page 0 when no progress is provided).
 *
 * @param {string} chaptersJson  JSON array of raw chapter descriptors.
 * @param {string} [progressJson] Serialised progress object; omit to start at page 0.
 */
async function loadContent(chaptersJson, progressJson = '') {
    _contentReady = false;

    const raw = JSON.parse(chaptersJson);

    // Create, load, and measure every iframe concurrently.
    chapters = await Promise.all(raw.map(_createChapter));

    // Assign consecutive startPage offsets and position iframes.
    _layoutChapters();

    // Allow one paint after positioning before reading node geometry.
    await _nextFrame();

    // Build per-chapter node caches with global-page coordinates.
    _buildSectionCache();

    bridge.onPagesCalculated(totalPages);

    if (progressJson) {
        const { chapterId, nodeIndex, nodeOffset = 0 } = JSON.parse(progressJson);
        _restoreProgress(chapterId, nodeIndex, nodeOffset);
    } else {
        goToPage(0, true, false);
    }

    _contentReady = true;

    setTimeout(() => {
        contentEl.style.opacity = '1';
        bridge.onContentReady();
    }, 0);
}

/**
 * Slides the container to reveal the given page and notifies the bridge.
 *
 * @param {number}  page       Target page index (clamped to [0, totalPages − 1]).
 * @param {boolean} forceEmit  Emit progress even when the page has not changed.
 * @param {boolean} animate    Apply a CSS transition (skipped for jumps > 2 pages).
 */
function goToPage(page, forceEmit = false, animate = true) {
    if (totalPages === 0) return;

    const newPage   = Math.max(0, Math.min(page, totalPages - 1));
    const doAnimate = animate && Math.abs(newPage - currentPage) <= 2;

    chapterContainer.style.transition = doAnimate ? TRANSITION_PAGE : 'none';
    chapterContainer.style.transform  = `translateX(${-newPage * colWidth}px)`;

    const changed = newPage !== currentPage;
    currentPage   = newPage;

    if (changed) {
        bridge.onPageChanged(currentPage + 1, totalPages);
        _emitProgress();
    } else if (forceEmit) {
        _emitProgress();
    }
}

/**
 * Searches every chapter iframe for an element with the given id and
 * navigates to its page.
 *
 * @param {string} id  The HTML id to locate.
 */
function navigateToId(id) {
    for (const ch of chapters) {
        const el = ch.el.contentDocument?.getElementById(id);
        if (!el) continue;
        goToPage(ch.startPage + _getNodeLocalPage(el, ch));
        return;
    }
}

/** Re-reports the current total page count to the bridge. */
function calculateTotalPages() {
    bridge.onPagesCalculated(totalPages);
}

/**
 * Recalculates page counts and restores the saved position after a viewport
 * resize (e.g. orientation change or font-size adjustment).
 */
async function onResize() {
    if (!_contentReady || chapters.length === 0) return;

    colWidth = _readColumnWidth();

    // Reset every iframe to the new viewport width and update CSS variables.
    for (const ch of chapters) {
        ch.el.style.width = `${colWidth}px`;
        _setIframeSizes(ch.el);
    }

    // Give the layout engine time to reflow before measuring.
    await _nextFrame();
    await _nextFrame();
    await new Promise(r => setTimeout(r, 50));

    // Re-measure and re-widen each iframe.
    for (const ch of chapters) {
        ch.pageCount      = _measureIframePageCount(ch.el);
        ch.el.style.width = `${ch.pageCount * colWidth}px`;
    }

    await _nextFrame();

    _layoutChapters();
    _buildSectionCache();

    bridge.onPagesCalculated(totalPages);

    if (lastProgress) {
        const { chapterId, nodeIndex, nodeOffset } = lastProgress;
        _restoreProgress(chapterId, nodeIndex, nodeOffset);
    } else {
        goToPage(Math.min(currentPage, totalPages - 1));
    }
}

// ─── Private — iframe creation ────────────────────────────────────────────────

/**
 * Creates and loads an iframe for a single chapter, waits for images and
 * fonts, measures the page count, and widens the iframe to fit all columns.
 *
 * @param {{ id: string, html: string }} raw  Raw chapter descriptor.
 * @returns {Promise<Object>} Resolved chapter descriptor with pageCount set.
 */
async function _createChapter(raw) {
    const iframe = document.createElement('iframe');

    // Start at viewport dimensions; widened to pageCount × colWidth after measurement.
    iframe.style.cssText =
        `position:absolute;top:0;left:0;` +
        `width:${colWidth}px;height:${window.innerHeight}px;` +
        `border:none;padding:0;margin:0;display:block;`;
    iframe.setAttribute('scrolling', 'no');

    chapterContainer.appendChild(iframe);

    await new Promise(resolve => {
        iframe.addEventListener('load', resolve, { once: true });
        iframe.srcdoc = _buildSrcdoc(raw);
    });

    const doc = iframe.contentDocument;

    // Inject CSS dimension variables before any layout measurement.
    _setIframeSizes(iframe);

    // Wait for images and fonts to load so layout measurements are accurate.
    await Promise.all([
        waitForImages(doc),
        doc.fonts?.ready ?? Promise.resolve(),
    ]);

    // Two extra frames for the multi-column layout to fully stabilise.
    await _nextFrame();
    await _nextFrame();

    const pageCount = _measureIframePageCount(iframe);

    // Widen the iframe so every column sits inside its rendered viewport.
    // The parent's translateX then exposes each column by sliding the container.
    iframe.style.width = `${pageCount * colWidth}px`;

    await _nextFrame();

    return { id: raw.id, el: iframe, pageCount, startPage: 0, nodes: [] };
}

/**
 * Builds the srcdoc string for a chapter iframe.
 *
 * The inline script handles anchor navigation and tap notifications for
 * programmatic or accessibility-driven clicks that reach the iframe directly.
 * Swipe gestures are captured by the parent's touch overlay and never arrive
 * here as touch events.
 *
 * @param {{ id: string, html: string }} chapter
 * @returns {string}
 */
function _buildSrcdoc(chapter) {
    const base    = new URL('epub/', window.location.href).href;
    const content = decodeB64(chapter.html);

    return `<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<base href="${base}">
<link rel="stylesheet" href="css/paged/paged.css">
<link rel="stylesheet" href="css/paged/paged_iframe.css">
</head>
<body>
<div id="pager"><section id="${chapter.id}">${content}</section></div>
<script src="js/paged/paged_iframe.js"></script>
</body>
</html>`;
}

// ─── Private — layout ─────────────────────────────────────────────────────────

/**
 * Assigns consecutive startPage offsets to chapters and positions their
 * iframes left-to-right using exact integer-page arithmetic.
 *
 * Must be called after every chapter's pageCount is up to date.
 */
function _layoutChapters() {
    let offsetPages = 0;
    totalPages = 0;

    for (const ch of chapters) {
        ch.startPage      = offsetPages;
        ch.el.style.left  = `${offsetPages * colWidth}px`;
        offsetPages      += ch.pageCount;
        totalPages       += ch.pageCount;
    }
}

/**
 * Counts the CSS-column pages in `iframe` using Range.getBoundingClientRect().
 *
 * The iframe must be at colWidth when this is called. CSS columns overflow
 * rightward in layout space; the Range captures their full extent with
 * sub-pixel precision. The −0.1 guard prevents float-rounding from inflating
 * the count by one.
 *
 * @param {HTMLIFrameElement} iframe
 * @returns {number}
 */
function _measureIframePageCount(iframe) {
    const doc   = iframe.contentDocument;
    const pager = doc?.getElementById('pager');
    if (!pager?.firstElementChild) return 1;

    const range = doc.createRange();
    range.selectNodeContents(pager);

    const rect = range.getBoundingClientRect();
    return Math.max(1, Math.ceil((rect.width - 0.1) / colWidth));
}

// ─── Private — section / node cache ──────────────────────────────────────────

/**
 * Populates ch.nodes for every chapter with global-page coordinates.
 *
 * Must be called after _layoutChapters() has set ch.startPage on every chapter
 * and after the iframes have been widened so node positions are stable.
 */
function _buildSectionCache() {
    for (const ch of chapters) {
        const section = ch.el.contentDocument?.getElementById(ch.id);
        if (!section) { ch.nodes = []; continue; }

        ch.nodes = [...section.querySelectorAll(BLOCK_SELECTOR)].map(node => {
            const localPage = _getNodeLocalPage(node, ch);
            const pageCount = _getNodePageCount(node);
            return {
                el:        node,
                startPage: ch.startPage + localPage,
                pageCount,
                endPage:   ch.startPage + localPage + pageCount - 1,
            };
        });
    }
}

/**
 * Returns the 0-based page index within the chapter on which `node` starts.
 *
 * Because navigation is driven entirely by the parent container's translateX,
 * the iframe itself is never translated. getBoundingClientRect() coordinates
 * are therefore in the iframe's own layout space — no correction is needed.
 *
 * @param {Element} node
 * @param {Object}  ch   Chapter descriptor.
 * @returns {number}
 */
function _getNodeLocalPage(node, ch) {
    const doc   = ch.el.contentDocument;
    const pager = doc.getElementById('pager');

    const pagerLeft = pager.getBoundingClientRect().left;
    const nodeLeft  = node.getBoundingClientRect().left;

    return Math.max(0, Math.floor((nodeLeft - pagerLeft) / colWidth));
}

/**
 * Returns the number of pages a node spans.
 *
 * In CSS multi-column layout the bounding rect width of a fragmented block
 * equals its total horizontal extent across all occupied columns
 * (pageCount × colWidth for a node spanning multiple columns).
 *
 * @param {Element} node
 * @returns {number}
 */
function _getNodePageCount(node) {
    return Math.max(1, Math.ceil(node.getBoundingClientRect().width / colWidth));
}

// ─── Private — progress ──────────────────────────────────────────────────────

/**
 * Locates the anchor node for currentPage and calls _reportProgress.
 *
 * All node geometry is read from the cached ch.nodes (global coordinates),
 * never from the live DOM on the hot path. When currentPage falls in a blank
 * gap between chapters the fallback anchor is the last node of the preceding
 * chapter.
 */
function _emitProgress() {
    let anchorChapterId  = chapters[0]?.id ?? null;
    let anchorNodeIndex  = 0;

    for (const ch of chapters) {
        const nodes = ch.nodes;
        if (!nodes.length) continue;

        const lastNode = nodes[nodes.length - 1];

        if (lastNode.endPage < currentPage) {
            // The entire chapter precedes currentPage — keep as running best.
            anchorChapterId = ch.id;
            anchorNodeIndex = nodes.length - 1;
            continue;
        }

        if (nodes[0].startPage > currentPage) break;

        // Binary search: last node with startPage ≤ currentPage.
        let lo = 0, hi = nodes.length - 1, found = 0;
        while (lo <= hi) {
            const mid = (lo + hi) >> 1;
            if (nodes[mid].startPage <= currentPage) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }

        const { startPage, pageCount, endPage } = nodes[found];
        if (currentPage <= endPage) {
            const offset = Math.min(0.999999,
                Math.max(0, (currentPage - startPage) / pageCount));
            _reportProgress(ch.id, found, offset);
            return;
        }

        anchorChapterId = ch.id;
        anchorNodeIndex = found;
    }

    // Fallback: currentPage is in a gap — report the last node seen before it.
    if (anchorChapterId) {
        const ch   = chapters.find(c => c.id === anchorChapterId);
        const node = ch.nodes[anchorNodeIndex];
        _reportProgress(
            anchorChapterId,
            anchorNodeIndex,
            (currentPage - node.startPage) / node.pageCount,
        );
    }
}

/**
 * Serialises progress and forwards it to the bridge.
 *
 * @param {string} chapterId
 * @param {number} nodeIndex
 * @param {number} nodeOffset  Fraction [0, 1) within the node.
 */
function _reportProgress(chapterId, nodeIndex, nodeOffset) {
    const globalProgress = totalPages > 1
        ? currentPage / (totalPages - 1)
        : (currentPage > 0 ? 1 : 0);

    lastProgress = { chapterId, nodeIndex, nodeOffset };
    bridge.onProgressChanged(globalProgress, chapterId,
        JSON.stringify({ chapterId, nodeIndex, nodeOffset }));
}

/**
 * Navigates to the page corresponding to a previously saved progress object.
 *
 * @param {string} chapterId
 * @param {number} nodeIndex
 * @param {number} [nodeOffset=0]  Fraction [0, 1) within the node.
 */
function _restoreProgress(chapterId, nodeIndex, nodeOffset = 0) {
    const ch = chapters.find(c => c.id === chapterId);
    if (!ch) { goToPage(0); return; }

    const target = ch.nodes[nodeIndex];
    if (!target) {
        const lastNode = ch.nodes[ch.nodes.length - 1];
        if (lastNode) goToPage(lastNode.startPage);
        else          navigateToId(chapterId);
        return;
    }

    const pageWithinNode = Math.max(0,
        Math.floor(nodeOffset * target.pageCount + 1e-9));

    goToPage(Math.min(target.startPage + pageWithinNode, totalPages - 1),
        true, false);
}

/**
 * Returns the integer CSS-pixel column width to use for all page calculations.
 *
 * Flooring to an integer prevents the rendering engine from making fractional
 * decisions that would desynchronise CSS columns from the Range measurements.
 *
 * @returns {number}
 */
function _readColumnWidth() {
    const width = contentEl
        ? contentEl.getBoundingClientRect().width
        : window.innerWidth;
    return Math.floor(width);
}

// ─── Private — swipe gestures ─────────────────────────────────────────────────

/**
 * Creates a full-viewport transparent overlay that captures all touch events
 * directly in the parent context.
 *
 * This mirrors the Shadow DOM version's approach (listening on the host element)
 * and avoids the cross-iframe forwarding that caused drag jitter. Touch events
 * fire in the same JS context as the style.transform writes, with no async
 * boundary in between.
 *
 * Taps (short, low-movement gestures) are forwarded to the iframe content
 * underneath via _handleTap(), which replicates the anchor-navigation and
 * TapDetector logic from the iframe's own click handler.
 */
function _initSwipeGesture() {
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;z-index:9999;';
    document.body.appendChild(overlay);

    let startX = 0, startY = 0, startTime = 0, dragging = false;

    overlay.addEventListener('touchstart', e => {
        const t = e.touches[0];
        startX    = t.clientX;
        startY    = t.clientY;
        startTime = Date.now();
        dragging  = true;
    }, { passive: true });

    overlay.addEventListener('touchmove', e => {
        if (!dragging) return;

        const t  = e.touches[0];
        const dx = t.clientX - startX;
        const dy = t.clientY - startY;

        // Only intercept horizontal gestures; let vertical ones fall through.
        if (Math.abs(dx) <= Math.abs(dy)) return;
        e.preventDefault();

        const atStart = dx > 0 && currentPage === 0;
        const atEnd   = dx < 0 && currentPage === totalPages - 1;
        if (atStart || atEnd) return;

        chapterContainer.style.transition = 'none';
        chapterContainer.style.transform  =
            `translateX(${-currentPage * colWidth + dx}px)`;
    }, { passive: false });

    overlay.addEventListener('touchend', e => {
        if (!dragging) return;
        dragging = false;

        const t       = e.changedTouches[0];
        const dx      = t.clientX - startX;
        const dy      = t.clientY - startY;
        const elapsed = Date.now() - startTime;

        // Tap: small movement and short duration — forward to iframe content.
        if (Math.hypot(dx, dy) < 10 && elapsed < 250) {
            _handleTap(t.clientX, t.clientY);
            return;
        }

        // Swipe: commit to next/previous page or snap back.
        const velocity = Math.abs(dx) / Math.max(1, elapsed);
        const passes   = Math.abs(dx) > colWidth * SWIPE_THRESHOLD || velocity > SWIPE_VELOCITY;

        chapterContainer.style.transition = TRANSITION_PAGE;

        if      (dx < 0 && passes && currentPage < totalPages - 1) goToPage(currentPage + 1);
        else if (dx > 0 && passes && currentPage > 0)               goToPage(currentPage - 1);
        else                                                         goToPage(currentPage);

        setTimeout(() => { chapterContainer.style.transition = 'none'; }, TRANSITION_DURATION);
    }, { passive: true });
}

// ─── Private — iframe sizing ──────────────────────────────────────────────────

/**
 * Injects --vw and --vh CSS custom properties into the iframe's root element.
 * paged.css uses these variables to set column-width and height on #pager.
 *
 * @param {HTMLIFrameElement} iframe
 */
function _setIframeSizes(iframe) {
    const w    = _readColumnWidth();
    const h    = window.innerHeight;
    const root = iframe.contentDocument?.documentElement;

    if (root) {
        root.style.setProperty('--vw', `${w}px`);
        root.style.setProperty('--vh', `${h}px`);
    }

    iframe.style.height = `${h}px`;
}

// ─── Utilities ────────────────────────────────────────────────────────────────

/** Resolves on the next animation frame. */
function _nextFrame() {
    return new Promise(resolve => requestAnimationFrame(resolve));
}