// Selects all block-level nodes used as progress anchors.
// Must match the scroll reader's selector so the same progress JSON is valid
// in both modes.
const BLOCK_SELECTOR = 'p, li, dt, dd, figcaption, blockquote, figure, h1, h2, h3, h4, h5, h6, img, svg';

function debounce(fn, delay) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

function decodeB64(b64) {
  const bytes = atob(b64);
  const arr = new Uint8Array(bytes.length);
  for (let i = 0; i < bytes.length; i++) arr[i] = bytes.charCodeAt(i);
  return new TextDecoder('utf-8').decode(arr);
}

function waitForImages(root) {
  const imgs = [...root.querySelectorAll('img')];
  const pending = imgs
    .filter(img => !img.complete)
    .map(img => new Promise(resolve => {
      img.addEventListener('load', resolve);
      img.addEventListener('error', resolve);
    }));
  return Promise.all(pending);
}

function waitForImagesAndFonts(root) {
  return Promise.all([
    waitForImages(root),
    document.fonts?.ready || Promise.resolve()
  ]);
}

function setupNavigationHandler(shadowRoot) {
  shadowRoot.addEventListener('click', (e) => {
    const a = e.target.closest('a[href]');
    if (!a) return;

    e.preventDefault();
    const href = a.getAttribute('href');

    if (href.startsWith('#')) {
      navigateToId(href.slice(1));
    } else {
      window.location.href = href;
    }
  });
}

function setupTapDetector() {
  let hasSelection = false;

  document.addEventListener('selectionchange', () => {
    const sel = document.getSelection();
    hasSelection = sel && sel.toString().length > 0;
  });

  document.addEventListener('click', (e) => {
    // If any element in the chain already handled this tap, bail out
    if (e.defaultPrevented) return;

    if (hasSelection) return;

    const ignored = [
      'a',
      'button',
      'input',
      'select',
      'textarea',
      'audio',        // epub3: inline audio controls
      'video',        // epub3: inline video controls
      'details',      // collapsible sections (epub3 / fixed-layout)
      'summary',      // toggle for <details>
      'label',        // form labels
      'object',       // iBooks interactive widgets (application/x-ibooks+widget)
      '[role="button"]',
      '[role="link"]',
      '[role="checkbox"]',
      '[role="switch"]',
      '[role="menuitem"]',
      '[onclick]',
      '[contenteditable]',
    ];

    if (ignored.some(selector => e.target.closest(selector))) return;

    const style = window.getComputedStyle(e.target);
    if (style.cursor === 'pointer') return;

    window.TapDetector?.notifyScreenTapped();
  });
}