package com.trackbool.bookreader.ui.components

import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader.PathHandler
import java.util.zip.ZipFile

class EpubAssetPathHandler(
    private val epubFilePath: String
) : PathHandler {

    override fun handle(path: String): WebResourceResponse? {
        return try {
            val zipFile = ZipFile(epubFilePath)
            val entry = zipFile.getEntry(path) ?: return null
            WebResourceResponse(
                mimeTypeForPath(path),
                "UTF-8",
                zipFile.getInputStream(entry)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun mimeTypeForPath(path: String): String = when {
        path.endsWith(".jpg", true) ||
                path.endsWith(".jpeg", true) -> "image/jpeg"
        path.endsWith(".png", true)   -> "image/png"
        path.endsWith(".gif", true)   -> "image/gif"
        path.endsWith(".webp", true)  -> "image/webp"
        path.endsWith(".svg", true)   -> "image/svg+xml"
        path.endsWith(".css", true)   -> "text/css"
        path.endsWith(".ttf", true)   -> "font/ttf"
        path.endsWith(".otf", true)   -> "font/otf"
        path.endsWith(".woff", true)  -> "font/woff"
        path.endsWith(".woff2", true) -> "font/woff2"
        else -> "application/octet-stream"
    }
}