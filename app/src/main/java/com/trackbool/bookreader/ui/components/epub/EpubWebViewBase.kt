package com.trackbool.bookreader.ui.components.epub

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Base64
import android.util.Log
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.ui.components.rememberAppAssetResolver
import com.trackbool.bookreader.ui.components.rememberEpubAssetResolver
import com.trackbool.bookreader.ui.epub.ASSET_BASE_URL
import com.trackbool.bookreader.ui.epub.EpubJavascriptInterface
import com.trackbool.bookreader.ui.epub.EpubWebViewClient
import com.trackbool.bookreader.ui.model.ChapterView
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("JavascriptInterface")
@Composable
internal fun EpubWebViewBase(
    book: Book,
    chapters: List<ChapterView>,
    assetFileName: String,
    modifier: Modifier = Modifier,
    extraJavascriptInterfaces: List<Pair<EpubJavascriptInterface, String>> = emptyList()
) {
    val appAssetResolver = rememberAppAssetResolver()
    val epubAssetResolver = rememberEpubAssetResolver(book.filePath)

    val readerHtml = remember(assetFileName) {
        appAssetResolver.resolve(assetFileName)?.bufferedReader().use { it?.readText() } ?: run {
            Log.e("EpubWebViewBase", "Failed to load asset: $assetFileName")
            ""
        }
    }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageReady by remember { mutableStateOf(false) }

    LaunchedEffect(pageReady, chapters) {
        if (!pageReady) return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect

        if (chapters.isEmpty()) {
            Log.w("EpubWebViewBase", "No chapters to inject!")
            return@LaunchedEffect
        }

        val chaptersJson = chapters.toChaptersJson()
        wv.evaluateJavascript("appendChapters('$chaptersJson');", null)

        if (book.documentPositionData.isNotEmpty()) {
            try {
                val json = JSONObject(book.documentPositionData)
                val chapterId = json.getString("chapterId")
                val nodeIndex = json.getInt("nodeIndex")
                wv.evaluateJavascript("restoreProgress('$chapterId', $nodeIndex);", null)
            } catch (e: Exception) {
                Log.e("EpubWebViewBase", "Failed to restore progress", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            epubAssetResolver.release()
            appAssetResolver.release()
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
                    }

                    extraJavascriptInterfaces.forEach { (obj, name) ->
                        addJavascriptInterface(obj, name)
                    }

                    webViewClient = EpubWebViewClient(
                        context = ctx,
                        epubAssetResolver = epubAssetResolver,
                        appAssetResolver = appAssetResolver,
                        onPageReady = { pageReady = true },
                    )

                    loadDataWithBaseURL(
                        ASSET_BASE_URL,
                        readerHtml,
                        "text/html",
                        "UTF-8",
                        null,
                    )
                }.also { webView = it }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun List<ChapterView>.toChaptersJson(): String =
    JSONArray(map { chapter ->
        val html = (chapter.content as? ChapterContent.Html)?.html.orEmpty()
        val htmlB64 = html.toByteArray(Charsets.UTF_8).toBase64()
        JSONObject().apply {
            put("id", chapter.id)
            put("html", htmlB64)
        }
    }).toString()

private fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)