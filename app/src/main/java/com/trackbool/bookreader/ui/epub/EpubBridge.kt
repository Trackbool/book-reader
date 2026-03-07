package com.trackbool.bookreader.ui.epub

import android.util.Log
import android.webkit.JavascriptInterface

class EpubBridge(
    private val onPagesCalculated: (totalPages: Int) -> Unit,
    private val onPageChanged: (current: Int, total: Int) -> Unit,
) : EpubJavascriptInterface {

    @JavascriptInterface
    fun onPagesCalculated(total: Int) {
        onPagesCalculated.invoke(total)
    }

    @JavascriptInterface
    fun onPageChanged(current: Int, total: Int) {
        onPageChanged.invoke(current, total)
    }

    @JavascriptInterface
    fun onDebugInfo(info: String) {
        Log.d("EpubBridge", info)
    }
}