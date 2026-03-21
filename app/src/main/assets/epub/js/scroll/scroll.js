// ─────────────────────────────────────────────────────────────────────────────
// EPUB Scroll Reader
//
// Each chapter renders in its own same-origin <iframe> (srcdoc). The iframe
// never scrolls — its height is stretched to fit its content so that a single
// window scroll drives the whole book. This isolates chapter CSS without
// requiring Shadow DOM, and lets click/tap listeners be attached directly to
// each iframe's document, mirroring the paged reader's approach.
//
// High-level flow:
//   1. init()         — locate #content container
//   2. loadContent()  — create iframes, wait for assets, restore progress
//   3. scroll events  — debounced _emitProgress() → bridge.onProgressChanged()
//
// Progress format (JSON):
//   { chapterId: string, nodeIndex: number, nodeOffset: number (0–1) }
//
// Bridge interface:
//   bridge.onContentReady()
//   bridge.onProgressChanged(globalProgress: number, chapterId: string, progressJson: string)
//
// Custom events dispatched to window (handled in epub_base.js):
//   epub:link:click   — { href: string }
//   epub:tap          — { target: Element }
//   epub:selection:change — { hasSelection: boolean }
// ─────────────────────────────────────────────────────────────────────────────

// Outer container that holds all chapter iframes stacked vertically.
let contentEl;

/**
 * Ordered list of loaded chapter descriptors.
 * @type {Array<{ id: string, el: HTMLIFrameElement }>}
 */
let chapters = [];

// ID of the chapter iframe currently at the top of the viewport.
// Updated by the IntersectionObserver; read by _emitProgress().
let currentChapterId = null;

// Guards against the IntersectionObserver overwriting currentChapterId while
// _restoreProgress() is executing a programmatic scroll.
// Without this, the observer's async callback can fire mid-scroll and reset
// currentChapterId to whichever chapter happens to be briefly visible during
// the scroll animation, corrupting the next _emitProgress() call.
let scrollLocked = false;

// ─── Initialisation ──────────────────────────────────────────────────────────

function init() {
    contentEl = document.getElementById('content');
}

document.addEventListener('DOMContentLoaded', init);

// ─── Public API ───────────────────────────────────────────────────────────────

// Entry point called by the native layer once the WebView has finished loading.
// Creates one iframe per chapter, waits for all assets, then either restores
// a previous reading position or starts from the top.
//
// chaptersJson — JSON array of { id: string, html: string (base64) }
// progressJson — optional JSON object { chapterId, nodeIndex, nodeOffset }
async function loadContent(chaptersJson, progressJson = '') {
    const raw = JSON.parse(chaptersJson);
    chapters  = await Promise.all(raw.map(_createChapter));

    _setupChapterObserver();
    _setupScrollTracking();

    if (progressJson) {
        const { chapterId, nodeIndex, nodeOffset = 0 } = JSON.parse(progressJson);
        _restoreProgress(chapterId, nodeIndex, nodeOffset);
    }

    await _nextFrame();

    setTimeout(() => {
        contentEl.style.opacity = '1';
        bridge.onContentReady();
    }, 0);
}

// Scrolls the viewport to the element with the given ID.
// Checks chapter IDs first, then searches inside each iframe's document.
// `offset` (px) leaves a small gap above the target so it is not flush with
// the top edge of the screen, improving readability.
function navigateToId(id, offset = 40) {
    const chapter = chapters.find(c => c.id === id);
    if (chapter) {
        window.scrollTo({ top: chapter.el.offsetTop - offset, behavior: 'instant' });
        return;
    }

    for (const ch of chapters) {
        const el = ch.el.contentDocument?.getElementById(id);
        if (!el) continue;
        const top = ch.el.offsetTop + el.getBoundingClientRect().top - offset;
        window.scrollTo({ top, behavior: 'instant' });
        return;
    }
}

function goToProgress(progress) {
    const total = document.documentElement.scrollHeight - window.innerHeight;
    const target = progress * total;
    window.scrollTo({ top: target, behavior: 'instant' });
}

// ─── Private — iframe creation ────────────────────────────────────────────────

// Creates and loads an iframe for a single chapter, waits for images and
// fonts, then stretches the iframe to its full content height so that the
// parent window scroll encompasses the entire book in one continuous strip.
//
// @param {{ id: string, html: string }} raw  Raw chapter descriptor.
// @returns {Promise<{ id: string, el: HTMLIFrameElement }>}
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

    // Block until all images and custom fonts have loaded so that
    // scrollHeight reflects the true laid-out height of the chapter.
    await waitForImagesAndFonts(doc);
    await _nextFrame();
    await _nextFrame();

    // Stretch the iframe to its content — it must never scroll internally.
    iframe.style.height = `${doc.documentElement.scrollHeight}px`;

    _attachIframeListeners(iframe, doc);

    return { id: raw.id, el: iframe };
}

// Builds the srcdoc string for a chapter iframe.
// Uses a <base> tag so relative asset URLs (images, fonts) resolve correctly
// against the epub/ directory on the same origin.
//
// @param {{ id: string, html: string }} chapter
// @returns {string}
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

// Registers interaction listeners on an iframe's document.
// All events are forwarded to the parent window as custom events so that
// epub_base.js remains the single place that decides how to handle them,
// mirroring the paged reader's approach.
//
// Selection state is tracked locally per iframe: if text is selected when
// a tap arrives, the tap clears the selection instead of firing epub:tap,
// preventing accidental navigation dismissals.
function _attachIframeListeners(iframe, doc) {
    let hasSelection = false;

    doc.addEventListener('selectionchange', () => {
        hasSelection = (doc.getSelection()?.toString().length ?? 0) > 0;
        window.dispatchEvent(new CustomEvent('epub:selection:change', {
            detail: { hasSelection }
        }));
    });

    doc.addEventListener('click', e => {
        // A tap while text is selected only clears the selection; it does not
        // trigger navigation or notify the native layer.
        if (hasSelection) {
            doc.getSelection()?.removeAllRanges();
            return;
        }

        const target = e.target;
        const link   = target.closest('a[href]');

        if (link) {
            e.preventDefault();
            window.dispatchEvent(new CustomEvent('epub:link:click', {
                detail: { href: link.getAttribute('href') }
            }));
            return;
        }

        if (!isInteractiveElement(target)) {
            window.dispatchEvent(new CustomEvent('epub:tap', {
                detail: { target }
            }));
        }
    });
}

// ─── Private — progress ───────────────────────────────────────────────────────

// Reads the current viewport position and reports it to the native layer.
// Called on every debounced scroll event.
function _emitProgress() {
    if (!currentChapterId) return;
    const chapter = chapters.find(c => c.id === currentChapterId);
    if (!chapter) return;

    const { nodeIndex, nodeOffset } = _getVisibleNodeData(chapter);

    bridge.onProgressChanged(
        _getGlobalProgress(),
        currentChapterId,
        JSON.stringify({ chapterId: currentChapterId, nodeIndex, nodeOffset })
    );
}

// Finds the first visible block node in the chapter and calculates how far
// into that node the viewport has scrolled.
//
// Node positions are read from the iframe's own getBoundingClientRect() and
// then offset by the iframe's position in the parent document, so that all
// coordinates are in the parent's viewport space.
//
// Returns:
//   nodeIndex  — index within the BLOCK_SELECTOR node list
//   nodeOffset — fraction of the node's height scrolled past the viewport top
//                (0 = node top visible, 1 = node bottom visible)
function _getVisibleNodeData(chapter) {
    const doc     = chapter.el.contentDocument;
    const section = doc.getElementById(chapter.id);
    if (!section) return { nodeIndex: 0, nodeOffset: 0 };

    const nodes     = [...section.querySelectorAll(BLOCK_SELECTOR)];
    // iframeTop translates iframe-relative rects into parent-viewport space.
    const iframeTop = chapter.el.getBoundingClientRect().top;

    let bestIndex = 0;
    for (let i = 0; i < nodes.length; i++) {
        const rect      = nodes[i].getBoundingClientRect();
        const absTop    = iframeTop + rect.top;
        const absBottom = iframeTop + rect.bottom;
        if (absBottom > 0 && absTop < window.innerHeight) {
            bestIndex = i;
            break;
        }
    }

    const best = nodes[bestIndex];
    let nodeOffset = 0;

    if (best) {
        const rect          = best.getBoundingClientRect();
        const absTop        = iframeTop + rect.top;
        const lineHeight    = _getEffectiveLineHeight(best);
        const linesTotal    = rect.height / lineHeight;
        const linesScrolled = -absTop / lineHeight;
        nodeOffset = parseFloat(
            Math.max(0, Math.min(1, linesScrolled / linesTotal)).toFixed(4)
        );
    }

    return { nodeIndex: bestIndex, nodeOffset };
}

// Scrolls the viewport to the position described by the saved progress object.
// This is the inverse of _getVisibleNodeData(): given a nodeIndex and a 0–1
// offset within that node, compute the absolute scroll position and jump to it.
//
// Node positions are obtained from the iframe's document and then translated
// into parent-document coordinates by adding the iframe's offsetTop.
function _restoreProgress(chapterId, nodeIndex, nodeOffset = 0) {
    // Lock the observer so it cannot overwrite currentChapterId while we scroll.
    scrollLocked     = true;
    currentChapterId = chapterId;

    const chapter = chapters.find(c => c.id === chapterId);
    if (!chapter) { scrollLocked = false; return; }

    const doc     = chapter.el.contentDocument;
    const section = doc.getElementById(chapterId);
    if (!section)  { scrollLocked = false; return; }

    const nodes  = [...section.querySelectorAll(BLOCK_SELECTOR)];
    const target = nodes[nodeIndex];
    if (!target)   { scrollLocked = false; return; }

    const rect       = target.getBoundingClientRect();
    const lineHeight = _getEffectiveLineHeight(target);
    const linesTotal = rect.height / lineHeight;

    // Inverse of the save formula:
    //   saved:    nodeOffset  = linesScrolled / linesTotal
    //   restore:  scrollTop   = iframeOffsetTop + nodeTop + nodeOffset * linesTotal * lineHeight
    const top = chapter.el.offsetTop + rect.top + nodeOffset * linesTotal * lineHeight;
    window.scrollTo({ top, behavior: 'instant' });

    requestAnimationFrame(() => {
        // Unlock after the scroll has settled and the observer has had one
        // opportunity to fire at the correct position.
        scrollLocked = false;
        _emitProgress();
    });
}

// ─── Private — observers and scroll ──────────────────────────────────────────

// Watches all chapter iframes and keeps currentChapterId pointing to whichever
// chapter is first (topmost) among those currently visible in the viewport.
//
// The IntersectionObserver targets the iframe elements directly. A data
// attribute carries the chapter ID so the callback can identify each entry
// without a reverse DOM lookup.
function _setupChapterObserver() {
    const ratios   = new Map();
    const orderMap = new Map(chapters.map((c, i) => [c.id, i]));

    const observer = new IntersectionObserver(entries => {
        entries.forEach(e => ratios.set(e.target.dataset.chapterId, e.intersectionRatio));

        const firstVisible = [...ratios.entries()]
            .filter(([, r]) => r > 0)
            .sort(([a], [b]) => (orderMap.get(a) ?? 0) - (orderMap.get(b) ?? 0))[0];

        // Ignore observer callbacks while a programmatic scroll is in progress
        // (see scrollLocked and _restoreProgress).
        if (firstVisible && !scrollLocked) {
            currentChapterId = firstVisible[0];
        }
    }, { threshold: [0, 0.01, 0.1, 0.25, 0.5, 0.75, 1.0] });

    chapters.forEach(ch => {
        ch.el.dataset.chapterId = ch.id;
        observer.observe(ch.el);
    });
}

// Attaches the scroll listener. The 300 ms debounce avoids flooding the bridge
// while the user is actively scrolling, without introducing a noticeable delay
// in the progress update once they stop.
// { passive: true } tells the browser we will never call preventDefault(),
// allowing it to optimise scroll performance on the compositor thread.
function _setupScrollTracking() {
    const emitDebounced = debounce(_emitProgress, 300);
    window.addEventListener('scroll', emitDebounced, { passive: true });
}

// ─── Utilities ────────────────────────────────────────────────────────────────

// Returns true when the user has scrolled to within 1px of the document end.
// Used to guarantee that the last paragraph always registers as fully read,
// regardless of sub-pixel rounding in scrollHeight.
function _isAtBottom() {
    const total = document.documentElement.scrollHeight - window.innerHeight;
    return window.scrollY >= total - 1;
}

// Returns a 0–1 value representing how far through the whole book the user
// has scrolled, used to drive a global progress indicator in the native UI.
function _getGlobalProgress() {
    const total = document.documentElement.scrollHeight - window.innerHeight;
    if (total <= 0) return 0;
    return _isAtBottom() ? 1 : Math.min(1, window.scrollY / total);
}

// Returns the effective line height of a node in CSS pixels.
//
// Why not just use fontSize?
//   epub stylesheets commonly set line-height to values like 1.5 or 24px.
//   Using fontSize as a proxy produces wrong line counts, which makes the
//   saved nodeOffset drift from the true reading position on restore.
//
// Why the "normal" fallback?
//   getComputedStyle().lineHeight returns the string "normal" when no explicit
//   value is set. The CSS spec defines "normal" as roughly 1.2× the font size,
//   so we apply that multiplier instead of parsing a number we cannot get.
function _getEffectiveLineHeight(node) {
    const style = getComputedStyle(node);
    const raw   = style.lineHeight;
    if (raw && raw !== 'normal') {
        const parsed = parseFloat(raw);
        if (!isNaN(parsed) && parsed > 0) return parsed;
    }
    return (parseFloat(style.fontSize) || 16) * 1.2;
}

// Resolves on the next animation frame.
function _nextFrame() {
    return new Promise(resolve => requestAnimationFrame(resolve));
}