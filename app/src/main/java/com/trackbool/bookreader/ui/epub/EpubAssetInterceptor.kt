package com.trackbool.bookreader.ui.epub

import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.trackbool.bookreader.domain.repository.AssetResolver

internal class EpubAssetInterceptor(
    private val assetResolver: AssetResolver,
) {

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        if (request.url.scheme != "epub") return null

        val raw = request.url.toString().removePrefix("epub://")
        val separatorIndex = raw.lastIndexOf('!')
        if (separatorIndex == -1) return null

        val entryPath = raw.substring(separatorIndex + 1)

        val stream = assetResolver.resolve(entryPath) ?: return null

        val extension = entryPath.substringAfterLast('.', missingDelimiterValue = "")
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"

        return WebResourceResponse(mimeType, "UTF-8", stream)
    }
}