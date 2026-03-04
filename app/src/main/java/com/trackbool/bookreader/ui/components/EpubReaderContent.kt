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
import androidx.compose.ui.viewinterop.AndroidView
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

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------
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
                    // Disable file access – content is injected programmatically.
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
                    /* data     */ buildReaderHtml(),
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

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)

private fun buildReaderHtml(): String = """
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: Georgia, 'Times New Roman', serif;
      font-size: 18px;
      line-height: 1.75;
      color: #1c1c1e;
      background: #fafaf8;
      padding: 24px 20px 48px;
      word-wrap: break-word;
      overflow-wrap: break-word;
      -webkit-text-size-adjust: 100%;
    }

    .chapter { margin-bottom: 56px; }

    .chapter-title {
      font-size: 1.25em;
      font-weight: 700;
      margin-bottom: 20px;
      text-align: center;
      color: #333;
    }

    .chapter-body p  { margin-bottom: 1em; text-indent: 1.5em; }
    .chapter-body p:first-child { text-indent: 0; }

    .chapter-body img {
      max-width: 100%;
      height: auto;
      display: block;
      margin: 16px auto;
    }

    .chapter-body a { color: #555; }

    #loader {
      text-align: center;
      padding: 32px 0;
      color: #aaa;
      font-style: italic;
      font-size: 0.9em;
    }
  </style>
  <script>
    function decodeB64(b64) {
      const bytes = atob(b64);
      const arr   = new Uint8Array(bytes.length);
      for (let i = 0; i < bytes.length; i++) arr[i] = bytes.charCodeAt(i);
      return new TextDecoder('utf-8').decode(arr);
    }

    function appendChapter(htmlB64) {
      const container = document.getElementById('content');
      const section   = document.createElement('section');
      section.className = 'chapter';
      section.innerHTML =
        '<div class="chapter-body">'  + decodeB64(htmlB64)  + '</div>';
      container.appendChild(section);
    }

    /* Throttled scroll listener – triggers chapter pre-fetch 400 px before end. */
    let ticking = false;
    window.addEventListener('scroll', function () {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(function () {
        const remaining = document.documentElement.scrollHeight
                        - window.scrollY
                        - window.innerHeight;
        if (remaining < 400) NativeApp.onNearBottom();
        ticking = false;
      });
    }, { passive: true });
  </script>
</head>
<body>
  <div id="content"></div>
  <div id="loader">Cargando…</div>
</body>
</html>
""".trimIndent()