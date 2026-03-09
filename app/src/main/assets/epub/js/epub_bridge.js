const bridge = {
  onContentReady: () => NativeApp.onContentReady(),
  onPageChanged: (page, total) => NativeApp.onPageChanged(page, total),
  onPagesCalculated: (total) => NativeApp.onPagesCalculated(total)
};
