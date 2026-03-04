package com.trackbool.bookreader.ui.components

import android.content.Intent
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

private val zipFileCache = ConcurrentHashMap<String, ZipFile>()

internal class EpubWebViewClient(
    private val onPageReady: () -> Unit,
) : WebViewClient() {

    override fun onPageFinished(view: WebView, url: String) {
        onPageReady()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        if (request.url.scheme != "epub") return null

        return try {
            val raw = request.url.toString().removePrefix("epub://")
            val separatorIndex = raw.lastIndexOf('!')
            if (separatorIndex == -1) return null

            val bookPath = raw.substring(0, separatorIndex)
            val entryPath = raw.substring(separatorIndex + 1)

            val zip = zipFileCache.getOrPut(bookPath) { ZipFile(bookPath) }
            val entry = zip.getEntry(entryPath) ?: return null

            val extension = entryPath.substringAfterLast('.', missingDelimiterValue = "")
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"

            WebResourceResponse(mimeType, "UTF-8", zip.getInputStream(entry))
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun closeAll() {
            zipFileCache.values.forEach { it.close() }
            zipFileCache.clear()
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val uri = request.url

        Log.d(javaClass.toString(), "URL clicked: $uri | scheme: ${uri.scheme}")
        when (uri.scheme) {
            "http", "https" -> {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                view.context.startActivity(intent)
            }
            "epub" -> {
                val id = uri.fragment ?: uri.lastPathSegment ?: return true
                Log.d(javaClass.toString(), "Id: $id")
                view.evaluateJavascript("scrollToId('$id');", null)
            }
        }

        return true
    }
}