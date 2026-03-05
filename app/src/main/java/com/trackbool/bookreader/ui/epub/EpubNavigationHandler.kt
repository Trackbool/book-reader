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
            "epub" -> {
                val id = uri.fragment ?: uri.lastPathSegment ?: return true
                Log.d(TAG, "Navigating to id: $id")
                view.evaluateJavascript("navigateToId('$id');", null)
                true
            }
            "http", "https" -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            }
            "mailto" -> {
                context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                true
            }
            "tel" -> {
                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                true
            }
            else -> {
                Log.w(TAG, "Blocked unhandled scheme: ${uri.scheme}")
                true
            }
        }
    }

    private companion object {
        const val TAG = "EpubNavigationHandler"
    }
}