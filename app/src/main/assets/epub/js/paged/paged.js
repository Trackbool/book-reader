// ─────────────────────────────────────────────────────────────────────────────
// EPUB Paged Reader
//
// Renders all book chapters inside a CSS-columns pager within a Shadow DOM.
// Pages are navigated by swipe gesture or programmatic goToPage() calls.
//
// Progress format (JSON):
//   { chapterId: string, nodeIndex: number, nodeOffset: number (0–1) }
//
//   nodeOffset is the fractional position within the anchor node's horizontal
//   extent (in CSS-column layout nodes flow left-to-right across columns):
//     0   = current page is the one where the node starts
//     0.5 = current page starts halfway through the node's width
//     ~1  = current page is at the very end of the node (stored as 0.999999)
//
//   Storing a fraction instead of a page count makes the position stable
//   across font-size changes: a node spanning 2 pages at small font may span
//   4 pages at large font. A fixed pageOffset would overshoot; nodeOffset
//   always means "this fraction into the node" regardless of how many pages
//   it currently occupies.
//
//   This mirrors the scroll reader's nodeOffset, keeping the JSON compatible
//   across both reading modes.
//
// Bridge interface:
//   bridge.onContentReady()
//   bridge.onPageChanged(currentPage: number, totalPages: number)
//   bridge.onPagesCalculated(totalPages: number)
//   bridge.onProgressChanged(globalProgress: number, progressJson: string)
// ─────────────────────────────────────────────────────────────────────────────

// Reference to the Shadow Root that hosts the #pager div.
let shadowRoot;

// Reference to #pager div
let pager;

// Cached column width. Invalidated on resize to avoid stale measurements.
let cachedColumnWidth = null;

// Cached section data built once after content loads. Each entry is:
//   { id: string, el: HTMLElement, nodes: Array<{ el: Element, startPage: number, pageCount: number, endPage: number }> }
// Avoids repeated querySelectorAll calls and DOM geometry reads on the hot navigation path.
let cachedSections = [];

// 0-based index of the page currently shown.
let currentPage = 0;

// Total number of CSS-column pages. Calculated after layout is stable.
let totalPages = 0;

// ─── Initialisation ──────────────────────────────────────────────────────────

function initShadow() {
    const host = document.getElementById('content');
    shadowRoot = host.attachShadow({ mode: 'open' });

    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'epub/css/paged/paged.css';
    shadowRoot.appendChild(link);

    pager = document.createElement('div');
    pager.id = 'pager';
    shadowRoot.appendChild(pager);

    _updateSizes();
    initSwipeGesture();
}

function _updateSizes() {
    const w = window.innerWidth;
    const h = window.innerHeight;
    shadowRoot.host.style.setProperty('--vw', `${w}px`);
    shadowRoot.host.style.setProperty('--vh', `${h}px`);
}

function initSwipeGesture() {
    const host = shadowRoot.host;
    let startX = 0;
    let startTime = 0;
    let dragging = false;

    const THRESHOLD = 0.2;
    const VELOCITY_THRESHOLD = 0.3;

    host.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
        startTime = Date.now();
        dragging = true;
    }, { passive: true });

    host.addEventListener('touchmove', (e) => {
        if (!dragging) return;

        const colWidth = getRealColumnWidth();
        const deltaX = e.touches[0].clientX - startX;

        if ((deltaX > 0 && currentPage === 0) ||
            (deltaX < 0 && currentPage === totalPages - 1)) return;

        pager.style.transition = 'none';
        pager.style.transform = `translateX(${-currentPage * colWidth + deltaX}px)`;
    }, { passive: true });

    host.addEventListener('touchend', (e) => {
        if (!dragging) return;
        dragging = false;

        const colWidth = getRealColumnWidth();
        const deltaX = e.changedTouches[0].clientX - startX;
        const velocity = Math.abs(deltaX) / (Date.now() - startTime);

        const isSwipeLeft  = deltaX < 0 && (Math.abs(deltaX) > colWidth * THRESHOLD || velocity > VELOCITY_THRESHOLD);
        const isSwipeRight = deltaX > 0 && (Math.abs(deltaX) > colWidth * THRESHOLD || velocity > VELOCITY_THRESHOLD);

        pager.style.transition = 'transform .3s ease';

        if      (isSwipeLeft  && currentPage < totalPages - 1) goToPage(currentPage + 1);
        else if (isSwipeRight && currentPage > 0)              goToPage(currentPage - 1);
        else                                                   goToPage(currentPage);

        setTimeout(() => { pager.style.transition = 'none'; }, 300);
    }, { passive: true });
}

// ─── Content loading ─────────────────────────────────────────────────────────

// Entry point called by Android once the WebView has finished loading the
// reader HTML. Injects all chapters, waits for assets, then either restores
// a previous reading position or starts from page 0.
//
// chaptersJson — JSON array of { id: string, html: string (base64) }
// progressJson — optional JSON object { chapterId, nodeIndex, nodeOffset }
async function loadContent(chaptersJson, progressJson = "") {
    const chapters = JSON.parse(chaptersJson);

    chapters.forEach(ch => {
        const section = document.createElement('section');
        section.id = ch.id;
        section.innerHTML = decodeB64(ch.html);
        pager.appendChild(section);
    });

    cachedColumnWidth = null;
    await waitForImagesAndFonts(shadowRoot);

    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            setTimeout(() => {
                buildSectionCache();
                calculateTotalPages();

                if (progressJson) {
                    const { chapterId, nodeIndex, nodeOffset = 0 } = JSON.parse(progressJson);
                    restoreProgress(chapterId, nodeIndex, nodeOffset);
                }

                setTimeout(() => {
                    shadowRoot.host.style.opacity = '1';
                    bridge.onContentReady();
                }, 0);
            }, 50);
        });
    });
}

// Builds cachedSections from the live DOM. Called once after content loads
// and again after a resize only if the section structure could have changed.
// Node geometry (startPage, pageCount, endPage) is computed here once and
// cached so emitProgress() never needs to read the DOM on the hot path.
function buildSectionCache() {
    cachedSections = [...pager.querySelectorAll('section[id]')].map(section => ({
        id: section.id,
        el: section,
        nodes: [...section.querySelectorAll(BLOCK_SELECTOR)].map(node => {
            const startPage = getNodeStartPage(node);
            const pageCount = getNodePageCount(node);
            return { el: node, startPage, pageCount, endPage: startPage + pageCount - 1 };
        })
    }));
}

// ─── Page navigation ─────────────────────────────────────────────────────────

// Translates the pager to show `page` and notifies Android.
// Also emits a progress update so Android can persist the new position.
function goToPage(page, forceEmit = false) {
    if (!pager) return;

    const newPage  = Math.max(0, Math.min(page, totalPages - 1));
    const colWidth = getRealColumnWidth();
    pager.style.transform = `translateX(${-newPage * colWidth}px)`;

    const pageChanged = newPage !== currentPage;
    currentPage = newPage;

    if (pageChanged) {
        bridge.onPageChanged(currentPage + 1, totalPages);
        emitProgress();
    } else if (forceEmit) {
        emitProgress();
    }
}

// Navigates to the element with the given ID by computing its page index.
function navigateToId(id) {
    const el = shadowRoot.getElementById(id);
    if (!pager || !el) return;

    const colWidth = getRealColumnWidth();

    const pagerRect = pager.getBoundingClientRect();
    const elRect = el.getBoundingClientRect();

    const relativeOffset = elRect.left - pagerRect.left;

    const page = Math.floor((relativeOffset + 2) / colWidth);
    goToPage(page);
}

// ─── Page count ──────────────────────────────────────────────────────────────

function getTotalPages() {
    if (!pager || !pager.firstElementChild) return 0;

    const colWidth = getRealColumnWidth();

    const range = document.createRange();
    range.selectNodeContents(pager);

    const rects = range.getBoundingClientRect();
    const preciseFullWidth = rects.width;
    const total = Math.ceil((preciseFullWidth - 0.1) / colWidth);

    return Math.max(1, total);
}

function calculateTotalPages() {
    totalPages = getTotalPages();
    bridge.onPagesCalculated(totalPages);
}

// ─── Column width helper ──────────────────────────────────────────────────────

function getRealColumnWidth() {
    if (cachedColumnWidth) return cachedColumnWidth;

    const width = pager.getBoundingClientRect().width;

    cachedColumnWidth = width > 0
        ? width
        : parseFloat(shadowRoot.host.style.getPropertyValue('--vw'));

    return cachedColumnWidth;
}

// ─── Node geometry helpers ────────────────────────────────────────────────────

// Returns the 0-based page index on which `node` starts.
//
// Uses offsetLeft traversal instead of getBoundingClientRect() because the
// pager is shifted with translateX: rects are viewport-relative and would be
// wrong when the pager is not at position 0. offsetLeft is always relative to
// the un-transformed layout.
function getNodeStartPage(node) {
    const colWidth = getRealColumnWidth();

    const pagerRect = pager.getBoundingClientRect();
    const nodeRect  = node.getBoundingClientRect();
    const absoluteOffset = nodeRect.left - pagerRect.left + currentPage * colWidth;

    return Math.floor(absoluteOffset / colWidth);
}

// Returns the number of pages the node spans (minimum 1).
//
// In CSS multi-column layout, a block element's offsetWidth is its total
// horizontal extent across all the columns it occupies. Dividing by colWidth
// gives the page count; ceil counts a partial last page as a full page.
function getNodePageCount(node) {
    const colWidth = getRealColumnWidth();
    const width    = node.getBoundingClientRect().width;
    return Math.max(1, Math.ceil(width / colWidth));
}

// ─── Progress — save ─────────────────────────────────────────────────────────

// Stores the last emitted progress so resize can restore from it without
// re-searching the DOM.
let lastProgress = null;

// Finds the anchor node for `currentPage` and reports progress to Android.
//
// Sections are scanned in order. For each section:
//   - If the entire section ends before currentPage, it is skipped in O(1)
//     and its last node is kept as the running best candidate (anchor fallback).
//   - If the entire section starts after currentPage, the search stops.
//   - Otherwise a binary search finds the last node with startPage <= currentPage.
//
// All geometry (startPage, pageCount, endPage) is read from cachedSections,
// never from the DOM.
//
// If the found node spans currentPage:
//   nodeOffset = (currentPage - startPage) / pageCount   ∈ [0, 1)
//
// nodeOffset is a fraction of the node's own span, so it stays meaningful
// after a font-size change that alters pageCount:
//   small font → node spans 2 pages, saved nodeOffset = 0.5 → page 1 of 2
//   large font → node spans 4 pages, restored nodeOffset = 0.5 → page 2 of 4
function emitProgress() {
    if (!pager) return;

    const sections = cachedSections;

    let anchorChapterId  = sections[0]?.id ?? null;
    let anchorNodeIndex  = 0;
    let anchorNodeOffset = 0;

    for (const section of sections) {
        const nodes = section.nodes;
        if (!nodes.length) continue;

        if (nodes[nodes.length - 1].endPage < currentPage) {
            anchorChapterId = section.id;
            anchorNodeIndex = nodes.length - 1;
            continue;
        }

        if (nodes[0].startPage > currentPage) break;

        let lo = 0, hi = nodes.length - 1, found = 0;
        while (lo <= hi) {
            const mid = (lo + hi) >> 1;
            if (nodes[mid].startPage <= currentPage) { found = mid; lo = mid + 1; }
            else hi = mid - 1;
        }

        const { startPage, pageCount, endPage } = nodes[found];

        if (currentPage <= endPage) {
            let nodeOffset = (currentPage - startPage) / pageCount;
            nodeOffset = Math.max(0, Math.min(0.999999, nodeOffset));
            _reportProgress(section.id, found, nodeOffset);
            return;
        }

        anchorChapterId = section.id;
        anchorNodeIndex = found;
    }

    // Fallback: currentPage has no node starting or spanning it (blank gap).
    // Report the last node seen before currentPage.
    if (anchorChapterId) {
        const section = cachedSections.find(s => s.id === anchorChapterId);
        const lastNode = section.nodes[anchorNodeIndex];

        // Calculate actual offset even if it's >= 1
        const { startPage, pageCount } = lastNode;
        anchorNodeOffset = (currentPage - startPage) / pageCount;

        _reportProgress(anchorChapterId, anchorNodeIndex, anchorNodeOffset);
    }
}

function _reportProgress(chapterId, nodeIndex, nodeOffset) {
    const globalProgress = totalPages > 1
        ? currentPage / (totalPages - 1)
        : (currentPage > 0 ? 1 : 0);

    const progressJson = JSON.stringify({ chapterId, nodeIndex, nodeOffset });

    lastProgress = { chapterId, nodeIndex, nodeOffset };
    bridge.onProgressChanged(globalProgress, progressJson);
}

// ─── Progress — restore ──────────────────────────────────────────────────────

// Navigates to the page described by { chapterId, nodeIndex, nodeOffset }.
//
// Inverse of emitProgress():
//   nodeStartPage = getNodeStartPage(target)        — live layout
//   nodePageCount = getNodePageCount(target)        — live layout
//   targetPage    = nodeStartPage + floor(nodeOffset * nodePageCount)
//
// Both measurements are taken from the current DOM, so the result is correct
// regardless of whether the font size has changed since progress was saved.
function restoreProgress(chapterId, nodeIndex, nodeOffset = 0) {
    const section = cachedSections.find(s => s.id === chapterId);

    if (!section) {
        // Chapter no longer exists (book updated?); start from page 0.
        goToPage(0);
        return;
    }

    const nodes  = section.nodes;
    const target = nodes[nodeIndex];

    if (!target) {
        // nodeIndex out of range (chapter was shortened?).
        // Land on the last available node of the chapter as a best-effort.
        const lastNode = nodes[nodes.length - 1];
        if (lastNode) goToPage(lastNode.startPage);
        else          navigateToId(chapterId);
        return;
    }

    const { startPage: nodeStartPage, pageCount: nodePageCount } = target;

    const EPS = 1e-9;

    // floor: land on the page that *starts* at nodeOffset within the node.
    let pageIndexWithinNode = Math.floor(nodeOffset * nodePageCount + EPS);
    pageIndexWithinNode = Math.max(0, pageIndexWithinNode);

    const targetPage = nodeStartPage + pageIndexWithinNode;

    goToPage(Math.min(targetPage, totalPages - 1), true);
}

// ─── Resize recovery ─────────────────────────────────────────────────────────
function onResize() {
    _updateSizes();
    _recalculateAfterResize();
}

// Called on every resize. currentPage is meaningless after a reflow because
// the column count changes, so we re-derive the correct page from the cached
// node anchor instead.
function _recalculateAfterResize() {
    cachedColumnWidth = null;
    buildSectionCache();
    calculateTotalPages();

    if (lastProgress) {
        const { chapterId, nodeIndex, nodeOffset } = lastProgress;
        restoreProgress(chapterId, nodeIndex, nodeOffset);
    } else {
        goToPage(Math.min(currentPage, totalPages - 1));
    }
}

// ─── Entry point ─────────────────────────────────────────────────────────────

function init() {
    initShadow();
    setupNavigationHandler(shadowRoot);
}

document.addEventListener('DOMContentLoaded', init);