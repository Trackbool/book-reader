const bridge = {
  onContentReady: () => NativeApp.onContentReady(),
  onPageChanged: (page, total) => NativeApp.onPageChanged(page, total),
  onPagesCalculated: (total) => NativeApp.onPagesCalculated(total),
  onProgressChanged: (readingProgress, chapterId, documentPositionData) => NativeApp.onProgressChanged(readingProgress, chapterId, documentPositionData),
  onDebugInfo: (info) => NativeApp.onDebugInfo(info)
};
