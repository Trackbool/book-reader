// ─────────────────────────────────────────────────────────────────────────────
// EPUB Scroll Reader
//
// Renders all book chapters as a single continuous scrollable document inside
// a Shadow DOM, and tracks reading progress at node-level precision so the
// reader can be restored to the exact paragraph and line where the user left off.
//
// High-level flow:
//   1. init()         — create Shadow DOM, wire up native navigation handler
//   2. loadContent()  — inject chapter HTML, wait for assets, restore progress
//   3. scroll events  — debounced emitProgress() → bridge.onProgressChanged()
//
// Progress format (JSON):
//   { chapterId: string, nodeIndex: number, nodeOffset: number (0–1) }
//
// Bridge interface:
//   bridge.onContentReady()
//   bridge.onProgressChanged(globalProgress: number, progressJson: string)
// ─────────────────────────────────────────────────────────────────────────────

// Selects all block-level nodes used as progress anchors.
// Inline elements are intentionally excluded: their rects change with font
// size and reflow, making them unreliable as stable reference points.
const BLOCK_SELECTOR = 'p, li, dt, dd, figcaption, blockquote, figure, h1, h2, h3, h4, h5, h6 img';

// Reference to the Shadow Root that hosts all chapter <section> elements.
// Kept at module scope so all functions can access it without threading it
// through every call.
let shadowRoot;

// ID of the <section> currently at the top of the viewport.
// Updated by the IntersectionObserver; read by emitProgress().
let currentChapterId = null;

// ─── Initialisation ──────────────────────────────────────────────────────────

// Creates the Shadow Root attached to #content and injects the reader stylesheet.
// Using Shadow DOM isolates epub CSS from the host page, preventing style leaks
// in both directions.
function initShadow() {
    const host = document.getElementById('content');
    shadowRoot = host.attachShadow({ mode: 'open' });

    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'epub/css/scroll/scroll.css';
    shadowRoot.appendChild(link);
}

// Entry point called by Android once the WebView has finished loading the
// reader HTML. Injects all chapters, waits for assets, then either restores
// a previous reading position or starts from the top.
//
// chaptersJson — JSON array of { id: string, html: string (base64) }
// progressJson — optional JSON object { chapterId, nodeIndex, nodeOffset }
async function loadContent(chaptersJson, progressJson = "") {
    const chapters = JSON.parse(chaptersJson);

    // Append each chapter as a <section id="…"> so we can observe them
    // individually with IntersectionObserver and navigate to them by ID.
    chapters.forEach(ch => {
        const section = document.createElement('section');
        section.id = ch.id;
        section.innerHTML = decodeB64(ch.html);
        shadowRoot.appendChild(section);
    });

    // Block until all images and custom fonts have loaded.
    // Without this, getBoundingClientRect() returns incorrect heights for
    // elements whose size depends on those resources.
    await waitForImagesAndFonts(shadowRoot);

    // Double rAF ensures we execute after the browser has performed a full
    // layout pass and painted at least one frame.
    //   • 1st rAF: queued before paint — dimensions may still be unstable
    //   • 2nd rAF: queued after paint  — layout is complete and rects are reliable
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            setupChapterObserver();
            setupScrollTracking();

            if (progressJson) {
                const { chapterId, nodeIndex, nodeOffset } = JSON.parse(progressJson);
                restoreProgress(chapterId, nodeIndex, nodeOffset);
            }

            bridge.onContentReady();
        });
    });
}

// ─── Navigation ──────────────────────────────────────────────────────────────

// Scrolls the viewport to the element with the given ID.
// `offset` (px) leaves a small gap above the target so it is not flush with
// the top edge of the screen, improving readability.
function navigateToId(id, offset = 40) {
    const el = shadowRoot.getElementById(id);
    if (!el) return;

    const top = el.getBoundingClientRect().top + window.scrollY - offset;
    window.scrollTo({ top, behavior: 'instant' });
}

// ─── Global progress ─────────────────────────────────────────────────────────

// Returns true when the user has scrolled to within 1px of the document end.
// Used to guarantee that the last paragraph always registers as fully read,
// regardless of sub-pixel rounding in scrollHeight.
function isAtBottom() {
    const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
    return window.scrollY >= totalHeight - 1;
}

// Returns a 0–1 value representing how far through the whole book the user has
// scrolled, used to drive a global progress bar in the Android UI.
// This is intentionally separate from the node-level progress used for
// restoration — it is coarser but cheaper to display.
function getGlobalProgress() {
    const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (totalHeight <= 0) return 0;
    return isAtBottom() ? 1 : Math.min(1, window.scrollY / totalHeight);
}

// ─── Line height helper ───────────────────────────────────────────────────────

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
function getEffectiveLineHeight(node) {
    const style = getComputedStyle(node);
    const raw = style.lineHeight;

    if (raw && raw !== 'normal') {
        const parsed = parseFloat(raw);
        if (!isNaN(parsed) && parsed > 0) return parsed;
    }

    // Fallback: CSS "normal" ≈ fontSize × 1.2
    return (parseFloat(style.fontSize) || 16) * 1.2;
}

// ─── Progress tracking ───────────────────────────────────────────────────────

// Finds the first visible block node in `section` and calculates how far
// into that node the viewport has scrolled, returning:
//   nodeIndex  — index within the BLOCK_SELECTOR node list
//   nodeOffset — fraction of the node's height that has scrolled past the
//                top of the viewport (0 = node top visible, 1 = node bottom visible)
function getVisibleNodeData(section) {
    const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];

    // Walk nodes until we find the first one that intersects the viewport.
    // "Intersects" means: not fully above (bottom > 0) and not fully below (top < innerHeight).
    let bestIndex = 0;
    for (let i = 0; i < nodes.length; i++) {
        const rect = nodes[i].getBoundingClientRect();
        if (rect.bottom > 0 && rect.top < window.innerHeight) {
            bestIndex = i;
            break;
        }
    }

    const bestNode = nodes[bestIndex];
    let nodeOffset = 0;

    if (bestNode) {
        const rect = bestNode.getBoundingClientRect();
        const lineHeight = getEffectiveLineHeight(bestNode);

        // linesTotal: how many lines tall the node is
        // linesScrolled: how many of those lines are above the viewport top
        //   rect.top is negative when the node has scrolled past the top edge
        const linesTotal = rect.height / lineHeight;
        const linesScrolled = -rect.top / lineHeight;

        nodeOffset = parseFloat(
            Math.max(0, Math.min(1, linesScrolled / linesTotal)).toFixed(4)
        );
    }

    return { nodeIndex: bestIndex, nodeOffset };
}

// Reads the current viewport position and reports it to Android via the bridge.
// Called on every debounced scroll event.
function emitProgress() {
    if (!currentChapterId) return;
    const section = shadowRoot.getElementById(currentChapterId);
    if (!section) return;

    const { nodeIndex, nodeOffset } = getVisibleNodeData(section);

    bridge.onProgressChanged(
        getGlobalProgress(),
        JSON.stringify({ chapterId: currentChapterId, nodeIndex, nodeOffset })
    );
}

// ─── Chapter observer ────────────────────────────────────────────────────────

// Guards against the IntersectionObserver overwriting currentChapterId while
// restoreProgress() is executing a programmatic scroll.
// Without this, the observer's async callback can fire mid-scroll and reset
// currentChapterId to whichever chapter happens to be briefly visible during
// the scroll animation, corrupting the next emitProgress() call.
let scrollLocked = false;

// Watches all <section> elements and keeps currentChapterId pointing to
// whichever chapter is first (topmost) in the document among those currently
// visible in the viewport.
function setupChapterObserver() {
    // ratios maps section ID → latest intersection ratio so we can filter
    // out fully-off-screen sections even after they leave the viewport.
    const ratios = new Map();

    const sections = [...shadowRoot.querySelectorAll('section[id]')];

    // orderMap lets us sort by document order using an O(1) lookup instead of
    // DOM comparison (compareDocumentPosition), which would be O(n) per sort.
    const orderMap = new Map(sections.map((s, i) => [s.id, i]));

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(e => ratios.set(e.target.id, e.intersectionRatio));

        const firstVisible = [...ratios.entries()]
            .filter(([, r]) => r > 0)
            .sort(([a], [b]) => (orderMap.get(a) ?? 0) - (orderMap.get(b) ?? 0))[0];

        // Ignore observer callbacks while a programmatic scroll is in progress
        // (see scrollLocked and restoreProgress).
        if (firstVisible && !scrollLocked) {
            currentChapterId = firstVisible[0];
        }
    }, { threshold: [0, 0.01, 0.1, 0.25, 0.5, 0.75, 1.0] });

    sections.forEach(s => observer.observe(s));
}

// ─── Scroll tracking ─────────────────────────────────────────────────────────

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

// Attaches the scroll listener. The 300 ms debounce avoids flooding the bridge
// while the user is actively scrolling, without introducing a noticeable delay
// in the progress update once they stop.
// { passive: true } tells the browser we will never call preventDefault(),
// allowing it to optimise scroll performance on the compositor thread.
function setupScrollTracking() {
    const emitDebounced = debounce(emitProgress, 300);
    window.addEventListener('scroll', emitDebounced, { passive: true });
}

// ─── Progress restoration ────────────────────────────────────────────────────

// Scrolls the viewport to the position described by the saved progress object.
// This is the inverse of getVisibleNodeData(): given a nodeIndex and a 0–1
// offset within that node, we compute the absolute scroll position and jump to it.
function restoreProgress(chapterId, nodeIndex, nodeOffset = 0) {
    // Lock the observer so it cannot overwrite currentChapterId while we scroll.
    scrollLocked = true;
    currentChapterId = chapterId;

    const section = shadowRoot.getElementById(chapterId);
    if (!section) {
        scrollLocked = false;
        return;
    }

    const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];
    const target = nodes[nodeIndex];
    if (!target) {
        scrollLocked = false;
        return;
    }

    const rect = target.getBoundingClientRect();
    const lineHeight = getEffectiveLineHeight(target);
    const linesTotal = rect.height / lineHeight;

    // Inverse of the save formula:
    //   saved:    nodeOffset = linesScrolled / linesTotal
    //   restore:  scrollTop  = nodeTop + nodeOffset * linesTotal * lineHeight
    const top = rect.top + window.scrollY + nodeOffset * linesTotal * lineHeight;
    window.scrollTo({ top, behavior: 'instant' });

    requestAnimationFrame(() => {
        // Unlock after the scroll has settled and the observer has had one
        // opportunity to fire at the correct position.
        scrollLocked = false;
        emitProgress();
    });
}

function init() {
    initShadow();
    setupNavigationHandler(shadowRoot);
}

document.addEventListener('DOMContentLoaded', init);