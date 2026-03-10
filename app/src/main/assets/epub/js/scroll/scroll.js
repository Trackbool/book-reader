const BLOCK_SELECTOR = 'p, li, dt, dd, figcaption, blockquote, figure, h1, h2, h3, h4, h5, h6 img';

let shadowRoot;
let currentChapterId = null;

function initShadow() {
    const host = document.getElementById('content');
    shadowRoot = host.attachShadow({ mode: 'open' });

    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'epub/css/scroll/scroll.css';
    shadowRoot.appendChild(link);
}

async function loadContent(chaptersJson) {
    const chapters = JSON.parse(chaptersJson);
    chapters.forEach(ch => {
        const section = document.createElement('section');
        section.id = ch.id;
        section.innerHTML = decodeB64(ch.html);
        shadowRoot.appendChild(section);
    });

    await waitForImagesAndFonts(shadowRoot);
    requestAnimationFrame(() => {
        readerHooks.emit('contentReady');
    });
}

function navigateToId(id, offset = 40) {
    const el = shadowRoot.getElementById(id);
    if (!el) return;

    const top = el.getBoundingClientRect().top + window.scrollY - offset;
    window.scrollTo({ top, behavior: 'instant' });
}

function isAtBottom() {
    const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
    return window.scrollY >= totalHeight - 1;
}

function getGlobalProgress() {
    const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
    if (totalHeight <= 0) return 0;
    return isAtBottom() ? 1 : Math.min(1, window.scrollY / totalHeight);
}

function getVisibleNodeIndex(section) {
    const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];
    const readingLine = window.innerHeight * 0.3;

    let bestIndex = 0;
    let bestDistance = Infinity;
    let foundFullyVisible = false;

    nodes.forEach((node, i) => {
        const rect = node.getBoundingClientRect();
        if (rect.bottom < 0 || rect.top > window.innerHeight) return;

        const fullyVisible = rect.top >= 0 && rect.bottom <= window.innerHeight;
        const distance = Math.abs(rect.top - readingLine);

        if (fullyVisible && !foundFullyVisible) {
            foundFullyVisible = true;
            bestDistance = distance;
            bestIndex = i;
        } else if (fullyVisible && distance < bestDistance) {
            bestDistance = distance;
            bestIndex = i;
        } else if (!foundFullyVisible && distance < bestDistance) {
            bestDistance = distance;
            bestIndex = i;
        }
    });

    return bestIndex;
}

function emitProgress() {
    if (!currentChapterId) return;
    const section = shadowRoot.getElementById(currentChapterId);
    if (!section) return;

    bridge.onProgressChanged(getGlobalProgress(), JSON.stringify({
        chapterId: currentChapterId,
        nodeIndex: getVisibleNodeIndex(section)
    }));
}

function setupChapterObserver() {
    const ratios = new Map();

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(e => ratios.set(e.target.id, e.intersectionRatio));

        let bestId = null;
        let bestRatio = -1;
        ratios.forEach((ratio, id) => {
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestId = id;
            }
        });

        if (bestId) currentChapterId = bestId;
    }, { threshold: [0, 0.1, 0.25, 0.5, 0.75, 1.0] });

    shadowRoot.querySelectorAll('section[id]').forEach(s => observer.observe(s));
}

function debounce(fn, delay) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

function setupScrollTracking() {
    const emitDebounced = debounce(emitProgress, 300);
    window.addEventListener('scroll', emitDebounced, { passive: true });
}

function restoreProgress(chapterId, nodeIndex) {
    currentChapterId = chapterId

    const section = shadowRoot.getElementById(chapterId);
    if (!section) return;

    const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];
    const target = nodes[nodeIndex];
    if (!target) return;

    const readingLine = window.innerHeight * 0.3;
    const top = target.getBoundingClientRect().top + window.scrollY - readingLine;
    window.scrollTo({ top, behavior: 'instant' });
    emitProgress();
};

readerHooks.on('contentReady', () => {
    setupChapterObserver();
    setupScrollTracking();
});

function init() {
    initShadow();
    setupNavigationHandler(shadowRoot);
}

document.addEventListener('DOMContentLoaded', init);