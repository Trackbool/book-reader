package com.trackbool.bookreader.ui.components.epub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.epub.EpubBridge
import com.trackbool.bookreader.ui.model.ChapterView

@Composable
internal fun EpubPagedReader(
    book: Book,
    chapters: List<ChapterView>,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    onContentReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bridge = remember {
        EpubBridge(
            onContentReady = onContentReady,
            onPagesCalculated = { total -> onTotalPagesCalculated(total) },
            onPageChanged = { current, _ -> onCurrentPageChanged(current) },
        )
    }

    EpubWebViewBase(
        book = book,
        chapters = chapters,
        assetFileName = "epub/epub_paged_template.html",
        modifier = modifier,
        extraJavascriptInterfaces = listOf(bridge to "NativeApp")
    )
}