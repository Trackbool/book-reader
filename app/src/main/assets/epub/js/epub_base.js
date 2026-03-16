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

function isInteractiveElement(target) {
	if (!(target instanceof Element)) return false;

	const interactiveSelectors = [
		'a',
		'button',
		'input',
		'select',
		'textarea',
		'audio',
		'video',
		'details',
		'summary',
		'label',
		'object',
		'[role="button"]',
		'[role="link"]',
		'[role="checkbox"]',
		'[role="switch"]',
		'[role="menuitem"]',
		'[onclick]',
		'[contenteditable]'
	];

	if (interactiveSelectors.some(sel => target.matches(sel) || target.closest(sel))) {
		return true;
	}

	const style = window.getComputedStyle(target);
	return style.cursor === 'pointer';
}

function dispatchTap(el) {
	const link = el.closest('a[href]');
	if (link) {
		const href = link.getAttribute('href');

		if (href && href.startsWith('#')) {
			navigateToId(href.slice(1));
		} else if (href) {
			window.location.href = href;
		}

		return;
	}

	if (isInteractiveElement(el)) {
		el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
		return;
	}

	window.TapDetector?.notifyScreenTapped();
}