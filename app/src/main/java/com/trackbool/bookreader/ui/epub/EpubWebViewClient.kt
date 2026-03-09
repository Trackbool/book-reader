package com.trackbool.bookreader.ui.epub

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.trackbool.bookreader.domain.repository.AssetResolver

internal class EpubWebViewClient(
    context: Context,
    epubAssetResolver: AssetResolver,
    appAssetResolver: AssetResolver,
    private val onPageReady: () -> Unit,
) : WebViewClient() {

    private val assetInterceptor = EpubAssetInterceptor(epubAssetResolver, appAssetResolver)
    private val navigationHandler = EpubNavigationHandler(context)

    override fun onPageFinished(view: WebView, url: String) {
        onPageReady()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? = assetInterceptor.intercept(request)

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = navigationHandler.handle(view, request.url)
}