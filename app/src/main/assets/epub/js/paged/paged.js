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

// Selects all block-level nodes used as progress anchors.
// Must match the scroll reader's selector so the same progress JSON is valid
// in both modes. Note the comma before `img` — see scroll reader bug #1.
const BLOCK_SELECTOR = 'p, li, dt, dd, figcaption, blockquote, figure, h1, h2, h3, h4, h5, h6, img';

// Reference to the Shadow Root that hosts the #pager div.
let shadowRoot;

// Cached column width. Invalidated on resize to avoid stale measurements.
let cachedColumnWidth = null;

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

    const pager = document.createElement('div');
    pager.id = 'pager';
    shadowRoot.appendChild(pager);

    function updateSizes() {
        const w = window.innerWidth;
        const h = window.innerHeight;
        shadowRoot.host.style.setProperty('--vw', `${w}px`);
        shadowRoot.host.style.setProperty('--vh', `${h}px`);
    }

    updateSizes();

    window.addEventListener('resize', () => {
        cachedColumnWidth = null;
        updateSizes();
        recalculateAfterResize();
    });

    initSwipeGesture();
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

        const pager = shadowRoot.getElementById('pager');
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

        const pager = shadowRoot.getElementById('pager');
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
    const pager = shadowRoot.getElementById('pager');

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
            calculateTotalPages();

            if (progressJson) {
                const { chapterId, nodeIndex, nodeOffset = 0 } = JSON.parse(progressJson);
                restoreProgress(chapterId, nodeIndex, nodeOffset);
            }

            setTimeout(() => {
                shadowRoot.host.style.opacity = '1';
                bridge.onContentReady();
            }, 0);
        });
    });
}

// ─── Page navigation ─────────────────────────────────────────────────────────

// Translates the pager to show `page` and notifies Android.
// Also emits a progress update so Android can persist the new position.
function goToPage(page, forceEmit = false) {
    const pager = shadowRoot.getElementById('pager');
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
    const pager = shadowRoot.getElementById('pager');
    const el    = shadowRoot.getElementById(id);
    if (!pager || !el) return;

    const colWidth = getRealColumnWidth();

    let offset = 0;
    let node = el;
    while (node && node !== pager) {
        offset += node.offsetLeft;
        node = node.offsetParent;
    }

    const page = Math.floor((offset + colWidth / 2) / colWidth);
    goToPage(page);
}

// ─── Page count ──────────────────────────────────────────────────────────────

function getTotalPages() {
    const pager = shadowRoot.getElementById('pager');
    if (!pager) return 0;

    const colWidth    = getRealColumnWidth();
    const scrollWidth = pager.scrollWidth;
    cachedColumnWidth = colWidth;

    const EPS = 0.5;
    return Math.max(1, Math.ceil((scrollWidth - EPS) / colWidth));
}

function calculateTotalPages() {
    totalPages = getTotalPages();
    bridge.onPagesCalculated(totalPages);
}

// ─── Column width helper ──────────────────────────────────────────────────────

function getRealColumnWidth() {
    if (cachedColumnWidth) return cachedColumnWidth;

    const pager = shadowRoot.getElementById('pager');
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
    const pager    = shadowRoot.getElementById('pager');
    const colWidth = getRealColumnWidth();

    let offset = 0;
    let el = node;
    while (el && el !== pager) {
        offset += el.offsetLeft;
        el = el.offsetParent;
    }

    return Math.floor(offset / colWidth);
}

// Returns the number of pages the node spans (minimum 1).
//
// In CSS multi-column layout, a block element's offsetWidth is its total
// horizontal extent across all the columns it occupies. Dividing by colWidth
// gives the page count; ceil counts a partial last page as a full page.
function getNodePageCount(node) {
    const colWidth = getRealColumnWidth();
    return Math.max(1, Math.ceil(node.offsetWidth / colWidth));
}

// ─── Progress — save ─────────────────────────────────────────────────────────

// Stores the last emitted progress so resize can restore from it without
// re-searching the DOM.
let lastProgress = null;

// Finds the anchor node for `currentPage` and reports progress to Android.
//
// For every node we compute:
//   nodeStartPage — first page the node appears on          (via offsetLeft)
//   nodePageCount — how many pages it spans                 (via offsetWidth)
//   nodeEndPage   — nodeStartPage + nodePageCount - 1
//
// If currentPage ∈ [nodeStartPage, nodeEndPage], this node is the anchor:
//   nodeOffset = (currentPage - nodeStartPage) / nodePageCount   ∈ [0, 1)
//
// nodeOffset is a fraction of the node's own span, so it stays meaningful
// after a font-size change that alters nodePageCount:
//   small font → node spans 2 pages, saved nodeOffset = 0.5 → page 1 of 2
//   large font → node spans 4 pages, restored nodeOffset = 0.5 → page 2 of 4  ✓
function emitProgress() {
    const pager = shadowRoot.getElementById('pager');
    if (!pager) return;

    const sections = [...pager.querySelectorAll('section[id]')];

    // Running best candidate: the node whose range most recently preceded
    // currentPage. Used as fallback when no node's range encloses it.
    let anchorChapterId  = sections[0]?.id ?? null;
    let anchorNodeIndex  = 0;
    let anchorNodeOffset = 0;

    for (const section of sections) {
        const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];

        for (let i = 0; i < nodes.length; i++) {
            const nodeStartPage = getNodeStartPage(nodes[i]);

            // Nodes are in document (left-to-right) order; stop scanning once
            // we've gone past the current page.
            if (nodeStartPage > currentPage) break;

            const nodePageCount = getNodePageCount(nodes[i]);
            const nodeEndPage   = nodeStartPage + nodePageCount - 1;

            if (currentPage <= nodeEndPage) {
                // currentPage is inside this node's range — exact anchor found.
                let nodeOffset = (currentPage - nodeStartPage) / nodePageCount;
                nodeOffset = Math.max(0, Math.min(0.999999, nodeOffset));

                _reportProgress(section.id, i, nodeOffset);
                return;
            }

            // Node ends before currentPage; keep as running candidate.
            anchorChapterId  = section.id;
            anchorNodeIndex  = i;
            anchorNodeOffset = 0.999999; // conceptually at the trailing edge of a passed node
        }
    }

    // Fallback: currentPage has no node starting or spanning it (blank gap).
    // Report the last node seen before currentPage.
    if (anchorChapterId) {
        const section = shadowRoot.getElementById(anchorChapterId);
        const nodes = section.querySelectorAll(BLOCK_SELECTOR);
        const lastNode = nodes[anchorNodeIndex];

        // Calculate actual offset even if it's >= 1
        const nodeStartPage = getNodeStartPage(lastNode);
        const nodePageCount = getNodePageCount(lastNode);
        anchorNodeOffset = (currentPage - nodeStartPage) / nodePageCount;

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
    const section = shadowRoot.getElementById(chapterId);

    if (!section) {
        // Chapter no longer exists (book updated?); start from page 0.
        goToPage(0);
        return;
    }

    const nodes  = [...section.querySelectorAll(BLOCK_SELECTOR)];
    const target = nodes[nodeIndex];

    if (!target) {
        // nodeIndex out of range (chapter was shortened?).
        // Land on the last available node of the chapter as a best-effort.
        const lastNode = nodes[nodes.length - 1];
        if (lastNode) goToPage(getNodeStartPage(lastNode));
        else          navigateToId(chapterId);
        return;
    }

    const nodeStartPage = getNodeStartPage(target);
    const nodePageCount = getNodePageCount(target);

    const EPS = 1e-9;

    // floor: land on the page that *starts* at nodeOffset within the node.
    let pageIndexWithinNode = Math.floor(nodeOffset * nodePageCount + EPS);
    pageIndexWithinNode = Math.max(0, pageIndexWithinNode);

    const targetPage = nodeStartPage + pageIndexWithinNode;

    goToPage(Math.min(targetPage, totalPages - 1), true);
}

// ─── Resize recovery ─────────────────────────────────────────────────────────

// Called on every resize. currentPage is meaningless after a reflow because
// the column count changes, so we re-derive the correct page from the cached
// node anchor instead.
function recalculateAfterResize() {
    cachedColumnWidth = null;
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