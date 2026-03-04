package com.trackbool.bookreader.ui.components

import android.util.Base64
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.ui.model.ChapterView

@Composable
fun EpubReaderContent(
    chapters: List<ChapterView>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val readerHtml = remember {
        context.assets
            .open("epub_reader_template.html")
            .bufferedReader()
            .use { it.readText() }
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
            wv.evaluateJavascript("appendChapter('$htmlB64');", null)
        }
        contentInjected = true
    }

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
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
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        background = null
                    }

                    webViewClient = EpubWebViewClient(
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

        if (!contentInjected) {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
            )
        }
    }
}

private fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)