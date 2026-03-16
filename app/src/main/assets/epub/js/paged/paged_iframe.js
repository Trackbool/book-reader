(function () {
    var p = window.parent;

    document.addEventListener('click', function (e) {
        if (e.defaultPrevented) return;

        var sel = document.getSelection();
        if (sel && sel.toString().length > 0) return;

        var a = e.target.closest('a[href]');
        if (a) {
            e.preventDefault();
            var h = a.getAttribute('href');
            if (h.charAt(0) === '#') p.navigateToId(h.slice(1));
            else p.location.href = h;
            return;
        }

        var ignored = [
            'button', 'input', 'select', 'textarea',
            '[role="button"]', '[role="link"]', '[onclick]', '[contenteditable]',
        ];
        if (ignored.some(function (s) { return e.target.closest(s); })) return;
        if (window.getComputedStyle(e.target).cursor === 'pointer') return;

        p.window.TapDetector && p.window.TapDetector.notifyScreenTapped();
    });
})();
