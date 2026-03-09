let shadowRoot;

function initShadow() {
  const host = document.getElementById('content');
  shadowRoot = host.attachShadow({ mode: 'open' });

  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'epub/css/scroll/scroll.css';
  shadowRoot.appendChild(link);
}

async function appendChapters(chaptersJson) {
  const chapters = JSON.parse(chaptersJson);
  chapters.forEach(ch => {
      const section = document.createElement('section');
      section.id = ch.id;
      section.innerHTML = decodeB64(ch.html);
      shadowRoot.appendChild(section);
  });

  await waitForImagesAndFonts(shadowRoot);
  requestAnimationFrame(() => {
      bridge.onContentReady();
  });
}

function navigateToId(id, offset = 40) {
  const el = shadowRoot.getElementById(id);
  if (!el) return;

  const top = el.getBoundingClientRect().top + window.scrollY - offset;

  window.scrollTo({
    top,
    behavior: 'instant'
  });
}

function init() {
  initShadow();
  setupNavigationHandler(shadowRoot);
}

document.addEventListener('DOMContentLoaded', init);
