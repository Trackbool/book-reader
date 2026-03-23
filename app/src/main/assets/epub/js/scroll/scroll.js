// ─────────────────────────────────────────────────────────────────────────────
// EPUB Scroll Reader
//
// Each chapter renders in its own same-origin <iframe> (srcdoc). The iframe
// never scrolls — its height is stretched to fit its content so that a single
// window scroll drives the whole book.
//
// Node cache format (parallel to the paged reader):
//   ch.nodes = Array<{ el: Element, top: number, height: number }>
//   where top/height are document-absolute coordinates (px from top of page).
//
// Progress format (JSON):
//   { chapterId: string, nodeIndex: number, nodeOffset: number [0, 1) }
//
//   nodeOffset = (scrollY − nodeTop) / nodeHeight
//   i.e. what fraction of the node has scrolled above the viewport top.
//   0 → node top is at or below viewport top.
//   1 → node bottom is at viewport top (node fully scrolled past).
//
// Bridge interface:
//   bridge.onContentReady()
//   bridge.onProgressChanged(globalProgress: number, chapterId: string, progressJson: string)
//
// Custom events dispatched to window (handled in epub_base.js):
//   epub:link:click        — { href: string }
//   epub:tap               — (no detail)
//   epub:selection:change  — { hasSelection: boolean }
// ─────────────────────────────────────────────────────────────────────────────

// BLOCK_SELECTOR is declared in epub_base.js.

// ─── State ────────────────────────────────────────────────────────────────────

/** Outer container that holds all chapter iframes stacked vertically. */
let contentEl;

/**
 * Ordered list of loaded chapter descriptors.
 * @type {Array<{
 *   id:    string,
 *   el:    HTMLIFrameElement,
 *   nodes: Array<{ el: Element, top: number, height: number }>
 * }>}
 */
let chapters = [];

/** Last serialised progress object; used by onResize to restore position. */
let lastProgress = null;

/**
 * Guards against the IntersectionObserver overwriting currentChapterId while
 * _restoreProgress() is executing a programmatic scroll.
 */
let scrollLocked = false;

/** ID of the topmost visible chapter (kept in sync by the IntersectionObserver). */
let currentChapterId = null;

// ─── Initialisation ───────────────────────────────────────────────────────────

function init() {
    contentEl = document.getElementById('content');
}

document.addEventListener('DOMContentLoaded', init);

// ─── Public API ───────────────────────────────────────────────────────────────

/**
 * Entry point called by the native layer once the WebView has finished loading.
 * Creates one iframe per chapter, waits for all assets, builds the node cache,
 * then either restores a previous reading position or starts from the top.
 *
 * @param {string} chaptersJson   JSON array of { id: string, html: string (base64) }
 * @param {string} [progressJson] JSON object { chapterId, nodeIndex, nodeOffset }
 * @param {string} [readerSettings] JSON object { fontSize: number, … }
 */
async function loadContent(chaptersJson, progressJson = '', readerSettings = '') {
    if (readerSettings) {
        const settings   = JSON.parse(readerSettings);
        _currentFontSize = settings.fontSize ?? 20;
    }

    const raw = JSON.parse(chaptersJson);
    chapters  = await Promise.all(raw.map(_createChapter));

    // Allow one paint after iframe sizing before reading node geometry.
    await _nextFrame();

    _buildSectionCache();

    _setupChapterObserver();
    _setupScrollTracking();

    if (progressJson) {
        const { chapterId, nodeIndex, nodeOffset = 0 } = JSON.parse(progressJson);
        _restoreProgress(chapterId, nodeIndex, nodeOffset);
    } else {
        _emitProgress();
    }

    await _nextFrame();

    setTimeout(() => {
        contentEl.style.opacity = '1';
        bridge.onContentReady();
    }, 0);
}

/**
 * Scrolls the viewport to the element with the given ID.
 * Checks chapter IDs first, then searches inside each iframe's document.
 *
 * @param {string} id
 * @param {number} [offset=40]  Visual gap above the target (px).
 */
function navigateToId(id, offset = 40) {
    const chapter = chapters.find(c => c.id === id);
    if (chapter) {
        window.scrollTo({ top: chapter.el.offsetTop - offset, behavior: 'instant' });
        return;
    }

    for (const ch of chapters) {
        const el = ch.el.contentDocument?.getElementById(id);
        if (!el) continue;
        const top = ch.el.offsetTop + el.getBoundingClientRect().top + window.scrollY - offset;
        window.scrollTo({ top, behavior: 'instant' });
        return;
    }
}

/**
 * Jumps to an absolute global-progress fraction [0, 1].
 *
 * @param {number} progress
 */
function goToProgress(progress) {
    const total = document.documentElement.scrollHeight - window.innerHeight;
    window.scrollTo({ top: progress * total, behavior: 'instant' });
}

/**
 * Recalculates iframe heights and restores the saved position after a viewport
 * resize (e.g. orientation change or font-size adjustment).
 */
async function onResize() {
    if (!chapters.length) return;

    // Reset every iframe so the browser can recalculate its content height,
    // and re-inject CSS variables in case viewport dimensions changed.
    for (const ch of chapters) {
        ch.el.style.height = 'auto';
        _setIframeVars(ch.el);
    }

    // Two frames for reflow + one tick for robustness (mirrors paged reader).
    await _nextFrame();
    await _nextFrame();
    await new Promise(r => setTimeout(r, 50));

    // Stretch each iframe to its new content height.
    for (const ch of chapters) {
        const doc = ch.el.contentDocument;
        if (doc) ch.el.style.height = `${doc.documentElement.scrollHeight}px`;
    }

    await _nextFrame();

    // Rebuild document-absolute node coordinates after layout has settled.
    _buildSectionCache();

    if (lastProgress) {
        const { chapterId, nodeIndex, nodeOffset } = lastProgress;
        _restoreProgress(chapterId, nodeIndex, nodeOffset);
    }
}

// ─── Private — iframe creation ─────────────────────────────────────────────────

/**
 * Creates and loads an iframe for a single chapter, waits for images and
 * fonts, then stretches the iframe to its full content height.
 *
 * @param {{ id: string, html: string }} raw
 * @returns {Promise<{ id: string, el: HTMLIFrameElement, nodes: [] }>}
 */
async function _createChapter(raw) {
    const iframe = document.createElement('iframe');
    iframe.style.cssText =
        'display:block;width:100%;border:none;padding:0;margin:0;overflow:hidden;';
    iframe.setAttribute('scrolling', 'no');
    contentEl.appendChild(iframe);

    await new Promise(resolve => {
        iframe.addEventListener('load', resolve, { once: true });
        iframe.srcdoc = _buildSrcdoc(raw);
    });

    const doc = iframe.contentDocument;

    _setIframeVars(iframe);

    // Block until all images and custom fonts have loaded so that
    // scrollHeight reflects the true laid-out height of the chapter.
    await waitForImagesAndFonts(doc);
    await _nextFrame();
    await _nextFrame();

    iframe.style.height = `${doc.documentElement.scrollHeight}px`;

    _attachIframeListeners(iframe, doc);

    return { id: raw.id, el: iframe, nodes: [] };
}

/**
 * Builds the srcdoc string for a chapter iframe.
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
<link rel="stylesheet" href="css/scroll/scroll.css">
</head>
<body>
<section id="${chapter.id}">${content}</section>
</body>
</html>`;
}

/**
 * Injects --vw, --vh and --reader-font-size CSS custom properties into the
 * iframe's root element, mirroring _setIframeSizes() in the paged reader.
 *
 * @param {HTMLIFrameElement} iframe
 */
function _setIframeVars(iframe) {
    const root = iframe.contentDocument?.documentElement;
    if (!root) return;
    root.style.setProperty('--vw',               `${window.innerWidth}px`);
    root.style.setProperty('--vh',               `${window.innerHeight}px`);
    root.style.setProperty('--reader-font-size', `${_currentFontSize}px`);
}

/**
 * Registers interaction listeners on an iframe's document and forwards all
 * events to the parent window as custom events (epub_base.js handles them).
 *
 * @param {HTMLIFrameElement} iframe
 * @param {Document}          doc
 */
function _attachIframeListeners(iframe, doc) {
    let hasSelection = false;

    doc.addEventListener('selectionchange', () => {
        hasSelection = (doc.getSelection()?.toString().length ?? 0) > 0;
        window.dispatchEvent(new CustomEvent('epub:selection:change', {
            detail: { hasSelection },
        }));
    });

    doc.addEventListener('click', e => {
        if (hasSelection) {
            doc.getSelection()?.removeAllRanges();
            return;
        }

        const target = e.target;
        const link   = target.closest('a[href]');

        if (link) {
            e.preventDefault();
            window.dispatchEvent(new CustomEvent('epub:link:click', {
                detail: { href: link.getAttribute('href') },
            }));
            return;
        }

        if (!isInteractiveElement(target)) {
            window.dispatchEvent(new CustomEvent('epub:tap'));
        }
    });
}

// ─── Private — node cache ──────────────────────────────────────────────────────

/**
 * Populates ch.nodes for every chapter with document-absolute coordinates.
 *
 * Must be called after all iframes have been stretched to their content height
 * (so that getBoundingClientRect() positions are stable).
 *
 * Mirrors _buildSectionCache() in the paged reader:
 *   paged  → coordinates are in page-index space  (startPage, pageCount, endPage)
 *   scroll → coordinates are in pixel space        (top, height)
 *
 * top    = distance from the very top of the document (px)
 * height = rendered height of the block element (px)
 */
function _buildSectionCache() {
    // scrollY at call time; added to rect.top to get document-absolute coords.
    const scrollY = window.scrollY;

    for (const ch of chapters) {
        const section = ch.el.contentDocument?.getElementById(ch.id);
        if (!section) { ch.nodes = []; continue; }

        // iframeTop: distance of the iframe's top edge from the document top.
        // Using getBoundingClientRect() + scrollY is reliable even when the
        // page is partially scrolled at measurement time.
        const iframeTop = ch.el.getBoundingClientRect().top + scrollY;

        ch.nodes = [...section.querySelectorAll(BLOCK_SELECTOR)].map(node => {
            const rect = node.getBoundingClientRect();
            return {
                el:     node,
                top:    iframeTop + rect.top,   // document-absolute top (px)
                height: Math.max(1, rect.height),
            };
        });
    }
}

// ─── Private — progress ───────────────────────────────────────────────────────

/**
 * Reads the current scroll position and reports progress to the native layer.
 *
 * Uses the cached ch.nodes to locate the first block node whose vertical span
 * contains the current viewport top (window.scrollY).
 *
 * Binary search mirrors _emitProgress() in the paged reader:
 *   paged  → finds the node where startPage ≤ currentPage ≤ endPage
 *   scroll → finds the node where top ≤ scrollY < top + height
 *
 * nodeOffset = (scrollY − nodeTop) / nodeHeight
 *   0       → viewport top is at or above node top
 *   ~1      → viewport top is near node bottom (node almost fully scrolled past)
 */
function _emitProgress() {
    const scrollY = window.scrollY;

    let anchorChapterId = chapters[0]?.id ?? null;
    let anchorNodeIndex = 0;

    for (const ch of chapters) {
        const nodes = ch.nodes;
        if (!nodes.length) continue;

        const lastNode = nodes[nodes.length - 1];

        // Chapter is entirely above the viewport — remember it as fallback anchor.
        if (lastNode.top + lastNode.height < scrollY) {
            anchorChapterId = ch.id;
            anchorNodeIndex = nodes.length - 1;
            continue;
        }

        // Chapter starts below the viewport — stop searching.
        if (nodes[0].top > scrollY + window.innerHeight) break;

        // Binary search: largest nodeIndex whose top ≤ scrollY.
        let lo = 0, hi = nodes.length - 1, found = 0;
        while (lo <= hi) {
            const mid = (lo + hi) >> 1;
            if (nodes[mid].top <= scrollY) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }

        const { top, height } = nodes[found];

        // Node spans the viewport top → this is the progress anchor.
        if (scrollY < top + height) {
            const offset = Math.min(0.999999,
                Math.max(0, (scrollY - top) / height));
            lastProgress = { chapterId: ch.id, nodeIndex: found, nodeOffset: offset };
            _reportProgress(ch.id, found, offset);
            return;
        }

        // scrollY is in a gap between this node and the next.
        anchorChapterId = ch.id;
        anchorNodeIndex = found;
    }

    // Fallback: use the last node that was above the viewport.
    if (anchorChapterId) {
        const ch   = chapters.find(c => c.id === anchorChapterId);
        const node = ch?.nodes[anchorNodeIndex];
        if (node) {
            const offset = Math.min(0.999999,
                Math.max(0, (scrollY - node.top) / node.height));
            lastProgress = { chapterId: anchorChapterId, nodeIndex: anchorNodeIndex, nodeOffset: offset };
            _reportProgress(anchorChapterId, anchorNodeIndex, offset);
        }
    }
}

/**
 * Serialises progress and forwards it to the bridge.
 * Mirrors _reportProgress() in the paged reader.
 *
 * @param {string} chapterId
 * @param {number} nodeIndex
 * @param {number} nodeOffset  Fraction [0, 1) of the node scrolled above the viewport top.
 */
function _reportProgress(chapterId, nodeIndex, nodeOffset) {
    bridge.onProgressChanged(
        _getGlobalProgress(),
        chapterId,
        JSON.stringify({ chapterId, nodeIndex, nodeOffset }),
    );
}

/**
 * Scrolls the viewport to the position described by a saved progress object.
 * Inverse of _emitProgress():
 *
 *   save:    nodeOffset = (scrollY    − nodeTop) / nodeHeight
 *   restore: scrollY    =  nodeOffset * nodeHeight + nodeTop
 *
 * Mirrors _restoreProgress() in the paged reader.
 *
 * @param {string} chapterId
 * @param {number} nodeIndex
 * @param {number} [nodeOffset=0]
 */
function _restoreProgress(chapterId, nodeIndex, nodeOffset = 0) {
    // Lock the observer so it cannot overwrite currentChapterId mid-scroll.
    scrollLocked     = true;
    currentChapterId = chapterId;

    const ch = chapters.find(c => c.id === chapterId);
    if (!ch) { scrollLocked = false; return; }

    const target = ch.nodes[nodeIndex];
    if (!target) {
        // Fallback: jump to the last node of the chapter, or to its top.
        const lastNode = ch.nodes[ch.nodes.length - 1];
        window.scrollTo({
            top:      lastNode ? lastNode.top : ch.el.offsetTop,
            behavior: 'instant',
        });
        scrollLocked = false;
        return;
    }

    window.scrollTo({
        top:      target.top + nodeOffset * target.height,
        behavior: 'instant',
    });

    requestAnimationFrame(() => {
        scrollLocked = false;
        // Report using the original node identity, not whatever node _emitProgress
        // would derive from scrollY after the layout change. This prevents
        // accumulated drift across successive resizes.
        _reportProgress(chapterId, nodeIndex, nodeOffset);
    });
}

// ─── Private — observers and scroll ───────────────────────────────────────────

/**
 * Watches all chapter iframes and keeps currentChapterId pointing to whichever
 * chapter is topmost among those currently visible in the viewport.
 */
function _setupChapterObserver() {
    const ratios   = new Map();
    const orderMap = new Map(chapters.map((c, i) => [c.id, i]));

    const observer = new IntersectionObserver(entries => {
        entries.forEach(e => ratios.set(e.target.dataset.chapterId, e.intersectionRatio));

        const firstVisible = [...ratios.entries()]
            .filter(([, r]) => r > 0)
            .sort(([a], [b]) => (orderMap.get(a) ?? 0) - (orderMap.get(b) ?? 0))[0];

        if (firstVisible && !scrollLocked) {
            currentChapterId = firstVisible[0];
        }
    }, { threshold: [0, 0.01, 0.1, 0.25, 0.5, 0.75, 1.0] });

    chapters.forEach(ch => {
        ch.el.dataset.chapterId = ch.id;
        observer.observe(ch.el);
    });
}

/**
 * Attaches the debounced scroll listener.
 * { passive: true } lets the browser optimise scroll on the compositor thread.
 */
function _setupScrollTracking() {
    const emitDebounced = debounce(_emitProgress, 300);
    window.addEventListener('scroll', () => {
        if (!scrollLocked) emitDebounced();
    }, { passive: true });
}

// ─── Utilities ────────────────────────────────────────────────────────────────

/**
 * Returns a 0–1 value representing how far through the whole book the user
 * has scrolled, used to drive a global progress indicator in the native UI.
 *
 * @returns {number}
 */
function _getGlobalProgress() {
    const total = document.documentElement.scrollHeight - window.innerHeight;
    if (total <= 0) return 0;
    return window.scrollY >= total - 1 ? 1 : Math.min(1, window.scrollY / total);
}

/** Resolves on the next animation frame. */
function _nextFrame() {
    return new Promise(resolve => requestAnimationFrame(resolve));
}