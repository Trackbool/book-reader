const readerHooks = {
  _listeners: {},
  on(event, fn) {
    (this._listeners[event] ??= []).push(fn);
  },
  emit(event, ...args) {
    (this._listeners[event] ?? []).forEach(fn => fn(...args));
  }
};

function debounce(fn, delay) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

function notifyReady() {
  bridge.onContentReady();
};

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
