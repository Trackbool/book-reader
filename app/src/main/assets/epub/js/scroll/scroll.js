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

function getVisibleNodeData(section) {
    const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];

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
        const fontSize = parseFloat(getComputedStyle(bestNode).fontSize) || 1;
        const linesTotal = rect.height / fontSize;
        const linesScrolled = -rect.top / fontSize;
        nodeOffset = parseFloat(
            Math.max(0, Math.min(1, linesScrolled / linesTotal)).toFixed(4)
        );
    }

    return { nodeIndex: bestIndex, nodeOffset };
}

function emitProgress() {
    if (!currentChapterId) return;
    const section = shadowRoot.getElementById(currentChapterId);
    if (!section) return;

    const { nodeIndex, nodeOffset } = getVisibleNodeData(section);
    bridge.onProgressChanged(getGlobalProgress(), JSON.stringify({
        chapterId: currentChapterId,
        nodeIndex,
        nodeOffset,
    }));
}

function setupChapterObserver() {
    const ratios = new Map();

    const sections = [...shadowRoot.querySelectorAll('section[id]')];
    const orderMap = new Map(sections.map((s, i) => [s.id, i]));

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(e => ratios.set(e.target.id, e.intersectionRatio));

        const firstVisible = [...ratios.entries()]
            .filter(([, r]) => r > 0)
            .sort(([a], [b]) => (orderMap.get(a) ?? 0) - (orderMap.get(b) ?? 0))[0];

        if (firstVisible) currentChapterId = firstVisible[0];
    }, { threshold: [0, 0.01, 0.1, 0.25, 0.5, 0.75, 1.0] });

    sections.forEach(s => observer.observe(s));
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

function restoreProgress(chapterId, nodeIndex, nodeOffset = 0) {
    currentChapterId = chapterId;

    const section = shadowRoot.getElementById(chapterId);
    if (!section) return;

    const nodes = [...section.querySelectorAll(BLOCK_SELECTOR)];
    const target = nodes[nodeIndex];
    if (!target) return;

    const rect = target.getBoundingClientRect();
    const fontSize = parseFloat(getComputedStyle(target).fontSize) || 1;
    const linesTotal = rect.height / fontSize;
    const top = rect.top + window.scrollY + nodeOffset * linesTotal * fontSize;

    window.scrollTo({ top, behavior: 'instant' });

    requestAnimationFrame(() => {
        emitProgress();
    });
}

readerHooks.on('contentReady', () => {
    setupChapterObserver();
    setupScrollTracking();
});

function init() {
    initShadow();
    setupNavigationHandler(shadowRoot);
}

document.addEventListener('DOMContentLoaded', init);