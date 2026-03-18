const SWIPE_THRESHOLD     = 0.22;
const SWIPE_VELOCITY      = 0.30;
const TRANSITION_PAGE     = 'transform .28s ease';
const DIRECTION_LOCK_PX   = 8;

window.TRANSITION_PAGE = TRANSITION_PAGE;

let _getCurrentPage;
let _getTotalPages;
let _getColWidth;
let _goToPage;
let _chapterContainer;

function initSwipeHandler(
    chaptersGetter,
    currentPageGetter,
    totalPagesGetter,
    colWidthGetter,
    chapterContainerEl,
    goToPageFn
) {
    _getCurrentPage   = currentPageGetter;
    _getTotalPages    = totalPagesGetter;
    _getColWidth      = colWidthGetter;
    _chapterContainer = chapterContainerEl;
    _goToPage         = goToPageFn;
}

function attachSwipeToIframe(iframe) {
    const doc = iframe.contentDocument;
    if (!doc) return;

    const el = doc.documentElement;

    let startX = 0;
    let startY = 0;
    let startT = 0;
    let baseX  = 0;
    let axis   = null;

    el.addEventListener('touchstart', e => {
        if (e.touches.length !== 1) return;
        const t = e.touches[0];
        startX = t.screenX;
        startY = t.screenY;
        startT = Date.now();
        axis   = null;
        baseX  = -_getCurrentPage() * _getColWidth();

        _chapterContainer.style.transition = 'none';
    }, { passive: true });

    el.addEventListener('touchmove', e => {
        if (e.touches.length !== 1) return;
        const t  = e.touches[0];
        const dx = t.screenX - startX;
        const dy = t.screenY - startY;

        if (axis === null) {
            if (Math.abs(dx) < DIRECTION_LOCK_PX && Math.abs(dy) < DIRECTION_LOCK_PX) return;
            axis = Math.abs(dx) >= Math.abs(dy) ? 'h' : 'v';
        }

        if (axis !== 'h') return;

        e.preventDefault();

        const page     = _getCurrentPage();
        const total    = _getTotalPages();
        const colWidth = _getColWidth();

        let newX = baseX + dx;
        if ((dx > 0 && page === 0) || (dx < 0 && page === total - 1)) {
            newX = baseX + dx * 0.25;
        }

        _chapterContainer.style.transform = `translateX(${newX}px)`;
    }, { passive: false });

    el.addEventListener('touchend', e => {
        if (axis !== 'h') {
            _chapterContainer.style.transition = 'none';
            _chapterContainer.style.transform  =
                `translateX(${-_getCurrentPage() * _getColWidth()}px)`;
            return;
        }

        if (e.changedTouches.length === 0) return;
        const t        = e.changedTouches[0];
        const dx       = t.screenX - startX;
        const elapsed  = Math.max(1, Date.now() - startT);
        const velocity = Math.abs(dx) / elapsed;
        const colWidth = _getColWidth();
        const page     = _getCurrentPage();
        const total    = _getTotalPages();
        const passes   = Math.abs(dx) > colWidth * SWIPE_THRESHOLD
                      || velocity > SWIPE_VELOCITY;

        if      (dx < 0 && passes && page < total - 1) _goToPage(page + 1);
        else if (dx > 0 && passes && page > 0)          _goToPage(page - 1);
        else                                             _goToPage(page);
    }, { passive: true });

    el.addEventListener('touchcancel', () => {
        _goToPage(_getCurrentPage());
    }, { passive: true });
}