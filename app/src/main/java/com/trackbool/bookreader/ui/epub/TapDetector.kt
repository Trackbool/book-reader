package com.trackbool.bookreader.ui.epub

import android.util.Log
import android.webkit.JavascriptInterface

class TapDetector(
    private val onScreenTapped: () -> Unit
) : EpubJavascriptInterface {

    @JavascriptInterface
    fun notifyScreenTapped() {
        Log.d("TapDetector", "notifyScreenTapped called")
        onScreenTapped.invoke()
    }
}
