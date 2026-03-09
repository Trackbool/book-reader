package com.trackbool.bookreader.ui.epub

import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.trackbool.bookreader.domain.repository.AssetResolver

internal class EpubAssetInterceptor(
    private val epubAssetResolver: AssetResolver,
    private val appAssetResolver: AssetResolver,
) {

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()

        if (url.startsWith(ASSET_BASE_URL)) {
            val assetPath = url.removePrefix(ASSET_BASE_URL)
            return loadAppAsset(assetPath)
        }

        if (request.url.scheme != "epub") return null

        val raw = request.url.toString().removePrefix("epub://")
        val separatorIndex = raw.lastIndexOf('!')
        if (separatorIndex == -1) return null

        val entryPath = raw.substring(separatorIndex + 1)

        val stream = epubAssetResolver.resolve(entryPath) ?: return null

        val extension = entryPath.substringAfterLast('.', missingDelimiterValue = "")
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"

        return WebResourceResponse(mimeType, "UTF-8", stream)
    }

    private fun loadAppAsset(path: String): WebResourceResponse? {
        val stream = appAssetResolver.resolve(path) ?: return null
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(path.substringAfterLast('.', ""))
            ?: "application/octet-stream"
        return WebResourceResponse(mimeType, "UTF-8", stream)
    }
}