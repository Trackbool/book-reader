package com.trackbool.bookreader.ui.components.epub

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Base64
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.ui.epub.EpubJavascriptInterface
import com.trackbool.bookreader.ui.epub.EpubWebViewClient
import com.trackbool.bookreader.ui.model.ChapterView

@SuppressLint("JavascriptInterface")
@Composable
internal fun EpubWebViewBase(
    book: Book,
    chapters: List<ChapterView>,
    assetFileName: String,
    modifier: Modifier = Modifier,
    extraJavascriptInterfaces: List<Pair<EpubJavascriptInterface, String>> = emptyList(),
    onChaptersInjected: (WebView) -> Unit = {},
    overlayContent: @Composable BoxScope.(contentInjected: Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val assetResolver = rememberEpubAssetResolver(book.filePath)

    val readerHtml = remember(assetFileName) {
        context.assets.open(assetFileName).bufferedReader().use { it.readText() }
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }
    var contentInjected by remember { mutableStateOf(false) }

    LaunchedEffect(pageReady) {
        if (!pageReady) return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect

        chapters.forEach { chapter ->
            val html = (chapter.content as? ChapterContent.Html)?.html.orEmpty()
            val htmlB64 = html.toByteArray(Charsets.UTF_8).toBase64()
            wv.evaluateJavascript("appendChapter('${chapter.id}', '$htmlB64');", null)
        }

        onChaptersInjected(wv)
        contentInjected = true
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            assetResolver.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    with(settings) {
                        javaScriptEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        allowFileAccess = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        setBackgroundColor(Color.TRANSPARENT)
                        background = null
                    }

                    extraJavascriptInterfaces.forEach { (obj, name) ->
                        addJavascriptInterface(obj, name)
                    }

                    webViewClient = EpubWebViewClient(
                        context = ctx,
                        assetResolver = assetResolver,
                        onPageReady = { pageReady = true },
                    )

                    loadDataWithBaseURL(
                        "epub://content/",
                        readerHtml,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }.also { webView = it }
            },
            update = { webView = it },
            modifier = Modifier.fillMaxSize(),
        )

        overlayContent(contentInjected)
    }
}

private fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)