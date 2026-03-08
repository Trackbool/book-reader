package com.trackbool.bookreader.ui.epub

import android.util.Log
import android.webkit.JavascriptInterface

class EpubBridge(
    private val onContentReady: () -> Unit,
    private val onPagesCalculated: (totalPages: Int) -> Unit,
    private val onPageChanged: (current: Int, total: Int) -> Unit,
) : EpubJavascriptInterface {

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
    fun onDebugInfo(info: String) {
        Log.d("EpubBridge", info)
    }
}