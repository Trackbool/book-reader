package com.trackbool.bookreader.ui.components.epub

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.components.LoadingIndicator
import com.trackbool.bookreader.ui.epub.EpubBridge
import com.trackbool.bookreader.ui.model.ChapterView

@Composable
internal fun EpubPagedReader(
    book: Book,
    chapters: List<ChapterView>,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bridge = remember {
        EpubBridge(
            onPagesCalculated = { total -> onTotalPagesCalculated(total) },
            onPageChanged = { current, _ -> onCurrentPageChanged(current) },
        )
    }

    EpubWebViewBase(
        book = book,
        chapters = chapters,
        assetFileName = "epub_paged_template.html",
        modifier = modifier,
        extraJavascriptInterfaces = listOf(bridge to "NativeApp"),
        onChaptersInjected = { wv ->
            wv.evaluateJavascript("calculateTotalPages();", null)
        },
        overlayContent = { contentInjected ->
            if (!contentInjected) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                )
            }
        },
    )
}