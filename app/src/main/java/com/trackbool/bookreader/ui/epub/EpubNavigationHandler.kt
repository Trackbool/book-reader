package com.trackbool.bookreader.ui.epub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView

internal class EpubNavigationHandler(private val context: Context) {

    fun handle(view: WebView, uri: Uri): Boolean {
        Log.d(TAG, "URL clicked: $uri | scheme: ${uri.scheme}")
        return when (uri.scheme) {
            "http", "https" -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            }
            "epub" -> {
                val id = uri.fragment ?: uri.lastPathSegment ?: return true
                Log.d(TAG, "Navigating to id: $id")
                view.evaluateJavascript("navigateToId('$id');", null)
                true
            }
            else -> false
        }
    }

    private companion object {
        const val TAG = "EpubNavigationHandler"
    }
}