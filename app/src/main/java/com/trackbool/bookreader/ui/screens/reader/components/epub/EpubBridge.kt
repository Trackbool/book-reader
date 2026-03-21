package com.trackbool.bookreader.ui.screens.reader.components.epub

import android.util.Log
import android.webkit.JavascriptInterface

class EpubBridge(
    private val onContentReady: () -> Unit,
    private val onPagesCalculated: (totalPages: Int) -> Unit,
    private val onPageChanged: (current: Int, total: Int) -> Unit,
    private val onProgressChanged: (readingProgress: Float, chapterId: String?, documentPositionData: String) -> Unit,
    private var executeJavascript: (String) -> Unit = {}
) : EpubJavascriptInterface {

    fun setExecuteJavascript(fn: (String) -> Unit) {
        executeJavascript = fn
    }

    @JavascriptInterface
    fun onContentReady() {
        Log.d("EpubBridge", "onContentReady called")
        onContentReady.invoke()
    }

    @JavascriptInterface
    fun onPagesCalculated(total: Int) {
        Log.d("EpubBridge", "onPagesCalculated called. Total pages: $total")
        onPagesCalculated.invoke(total)
    }

    @JavascriptInterface
    fun onPageChanged(current: Int, total: Int) {
        Log.d("EpubBridge", "onPageChanged called. Page: $current/$total")
        onPageChanged.invoke(current, total)
    }

    @JavascriptInterface
    fun onProgressChanged(readingProgress: Float, chapterId: String?, documentPositionData: String) {
        Log.d(
            "EpubBridge",
            "onProgressChanged called. " +
                    "Reading progress: $readingProgress. " +
                    "ChapterId: $chapterId. " +
                    "Document position data: $documentPositionData"
        )
        onProgressChanged.invoke(readingProgress, chapterId, documentPositionData)
    }

    @JavascriptInterface
    fun goToProgress(progress: Float) {
        Log.d("EpubBridge", "goToProgress called with progress: $progress")
        executeJavascript("goToProgress($progress)")
    }

    @JavascriptInterface
    fun goToPage(page: Int) {
        Log.d("EpubBridge", "goToPage called with page: $page")
        executeJavascript("goToPage(${page - 1})")
    }

    @JavascriptInterface
    fun setFontSize(size: Float) {
        Log.d("EpubBridge", "setFontSize called with size: $size")
        executeJavascript("setFontSize($size)")
    }

    @JavascriptInterface
    fun onDebugInfo(info: String) {
        Log.d("EpubBridge", info)
    }
}
