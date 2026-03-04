package com.trackbool.bookreader.ui.components

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.ui.model.ChapterView

// ---------------------------------------------------------------------------
// State holder – stable reference captured by the JS bridge.
// Updated on every recomposition so the bridge always reads current values.
// ---------------------------------------------------------------------------
private class EpubReaderState {
    var hasMoreChapters: Boolean = true
    var isLoadingMore: Boolean = false
    var onLoadMore: () -> Unit = {}
}

private class EpubJsBridge(private val state: EpubReaderState) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onNearBottom() {
        if (state.hasMoreChapters && !state.isLoadingMore) {
            mainHandler.post { state.onLoadMore() }
        }
    }
}

@Composable
fun EpubReaderContent(
    book: Book,
    chapters: List<ChapterView>,
    hasMoreChapters: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember { EpubReaderState() }
    val context = LocalContext.current
    val readerHtml = remember {
        context.assets
            .open("epub_reader_template.html")
            .bufferedReader()
            .use { it.readText() }
    }
    state.hasMoreChapters = hasMoreChapters
    state.isLoadingMore = isLoadingMore
    state.onLoadMore = onLoadMore

    val injectedCount = remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }

    LaunchedEffect(pageReady, chapters.size) {
        if (!pageReady) return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect

        chapters.drop(injectedCount.intValue).forEach { chapter ->
            val html = (chapter.content as? ChapterContent.Html)?.html.orEmpty()
            val htmlB64 = html.toByteArray(Charsets.UTF_8).toBase64()
            wv.evaluateJavascript("appendChapter('$htmlB64');", null)
        }
        injectedCount.intValue = chapters.size
    }

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                with(settings) {
                    javaScriptEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(false)
                    allowFileAccess = false
                }

                addJavascriptInterface(EpubJsBridge(state), "NativeApp")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        pageReady = true
                    }

                    // Block external navigation; epub:// URIs are fine.
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = request.url.scheme != "epub"
                }

                loadDataWithBaseURL(
                    /* baseUrl  */ "epub://content/",
                    /* data     */ readerHtml,
                    /* mimeType */ "text/html",
                    /* encoding */ "UTF-8",
                    /* histUrl  */ null,
                )
            }.also { webView = it }
        },
        // Keep the reference fresh across recompositions.
        update = { webView = it },
        modifier = modifier,
    )
}

private fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)