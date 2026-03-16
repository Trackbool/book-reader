const SWIPE_THRESHOLD        = 0.20;
const SWIPE_VELOCITY         = 0.30;
const TRANSITION_PAGE        = 'transform .3s ease';
const TRANSITION_DURATION    = 300;
const TAP_MOVE_THRESHOLD     = 8;
const YIELD_FOR_SELECTION_MS = 120;

window.TRANSITION_PAGE = TRANSITION_PAGE;

let getChapters;
let getCurrentPage;
let getTotalPages;
let getColWidth;
let goToPageFn;

function initSwipeHandler(
    chaptersGetter, currentPageGetter, totalPagesGetter,
    colWidthGetter, chapterContainerEl, goToPageFnParam
) {
    getChapters    = chaptersGetter;
    getCurrentPage = currentPageGetter;
    getTotalPages  = totalPagesGetter;
    getColWidth    = colWidthGetter;
    goToPageFn     = goToPageFnParam;

    _initSwipeGesture(chapterContainerEl);
}

function _currentChapter() {
    const page = getCurrentPage();
    for (const ch of getChapters()) {
        if (page >= ch.startPage && page < ch.startPage + ch.pageCount) return ch;
    }
    return getChapters()[0] ?? null;
}

function _elementAtViewport(vx, vy) {
    const ch = _currentChapter();
    if (!ch?.el?.contentDocument) return null;
    const offsetX = (getCurrentPage() - ch.startPage) * getColWidth();
    return ch.el.contentDocument.elementFromPoint(vx + offsetX, vy);
}

function _hasActiveSelection() {
    for (const ch of getChapters()) {
        const sel = ch.el?.contentDocument?.getSelection();
        if (sel?.toString().length > 0) return true;
    }
    return false;
}

function _clearAllSelections() {
    for (const ch of getChapters()) {
        ch.el?.contentDocument?.getSelection()?.removeAllRanges();
    }
}

function _forwardTap(vx, vy) {
    const ch  = _currentChapter();
    if (!ch?.el?.contentDocument) return;

    const el = _elementAtViewport(vx, vy);
    if (!el) return;

    dispatchTap(el);
}

function _initSwipeGesture(chapterContainer) {

    const overlay = document.createElement('div');
    overlay.id = 'swipe-overlay';
    overlay.style.cssText =
        'position:fixed;top:0;left:0;width:100%;height:100%;' +
        'z-index:9999;touch-action:pan-y;pointer-events:auto;' +
        'background:transparent;-webkit-tap-highlight-color:transparent;';
    document.body.appendChild(overlay);

    let state      = 'idle';
    let startX     = 0;
    let startY     = 0;
    let startTime  = 0;
    let yieldTimer = null;
    let hadSelectionOnStart = false;

    function cancelYieldTimer() {
        if (yieldTimer) { clearTimeout(yieldTimer); yieldTimer = null; }
    }

    function yield_() { overlay.style.pointerEvents = 'none'; }
    function reclaim() { overlay.style.pointerEvents = 'auto'; }

    function snapToPage() {
        chapterContainer.style.transition = TRANSITION_PAGE;
        goToPageFn(getCurrentPage());
        setTimeout(reclaim, TRANSITION_DURATION);
    }

    overlay.addEventListener('touchstart', e => {
        if (e.touches.length !== 1) {
            cancelYieldTimer();
            if (state === 'horizontal') snapToPage();
            else reclaim();
            state = 'idle';
            return;
        }

        const t   = e.touches[0];
        startX    = t.clientX;
        startY    = t.clientY;
        startTime = Date.now();

        hadSelectionOnStart = _hasActiveSelection();
        if (hadSelectionOnStart) {
            _clearAllSelections();
        }

        state = 'pending';

        yieldTimer = setTimeout(() => {
            yieldTimer = null;
            if (state !== 'pending') return;
            state = 'yielded';
            yield_();
        }, YIELD_FOR_SELECTION_MS);

    }, { passive: true });

    overlay.addEventListener('touchmove', e => {
        if (e.touches.length !== 1) return;
        if (state === 'yielded' || state === 'idle') return;

        const t     = e.touches[0];
        const dx    = t.clientX - startX;
        const dy    = t.clientY - startY;
        const absDx = Math.abs(dx);
        const absDy = Math.abs(dy);

        if (state === 'pending') {
            if (absDx < TAP_MOVE_THRESHOLD && absDy < TAP_MOVE_THRESHOLD) return;
            cancelYieldTimer();
            if (absDx >= absDy) {
                reclaim();
                state = 'horizontal';
            } else {
                state = 'yielded';
                return;
            }
        }

        if (state !== 'horizontal') return;

        e.preventDefault();

        const page     = getCurrentPage();
        const total    = getTotalPages();
        const colWidth = getColWidth();
        if ((dx > 0 && page === 0) || (dx < 0 && page === total - 1)) return;

        chapterContainer.style.transition = 'none';
        chapterContainer.style.transform  = `translateX(${-page * colWidth + dx}px)`;

    }, { passive: false });

    overlay.addEventListener('touchend', e => {
        cancelYieldTimer();
        const prevState = state;
        state = 'idle';

        if (prevState === 'yielded') {
            reclaim();
            return;
        }

        if (prevState === 'pending') {
            if (e.changedTouches.length === 0) { reclaim(); return; }
            const t = e.changedTouches[0];
            if (hadSelectionOnStart || _hasActiveSelection()) {
                _clearAllSelections();
            } else {
                _forwardTap(t.clientX, t.clientY);
            }
            reclaim();
            return;
        }

        if (prevState === 'horizontal') {
            if (e.changedTouches.length === 0) { reclaim(); return; }
            const t        = e.changedTouches[0];
            const dx       = t.clientX - startX;
            const elapsed  = Math.max(1, Date.now() - startTime);
            const velocity = Math.abs(dx) / elapsed;
            const colWidth = getColWidth();
            const page     = getCurrentPage();
            const total    = getTotalPages();
            const passes   = Math.abs(dx) > colWidth * SWIPE_THRESHOLD
                          || velocity > SWIPE_VELOCITY;

            chapterContainer.style.transition = TRANSITION_PAGE;

            if      (dx < 0 && passes && page < total - 1) goToPageFn(page + 1);
            else if (dx > 0 && passes && page > 0)          goToPageFn(page - 1);
            else                                             goToPageFn(page);

            setTimeout(reclaim, TRANSITION_DURATION);
            return;
        }

        reclaim();
    }, { passive: true });

    overlay.addEventListener('touchcancel', () => {
        cancelYieldTimer();
        if (state === 'horizontal') snapToPage();
        else reclaim();
        state = 'idle';
    }, { passive: true });
}