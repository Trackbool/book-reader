package com.trackbool.bookreader.ui.screens.reader.components.epub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.common.model.ChapterView
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun EpubPagedReader(
    book: Book,
    chapters: List<ChapterView>,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    goToPage: SharedFlow<Int>,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    onScreenTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bridge = remember {
        EpubBridge(
            onContentReady = onContentReady,
            onPagesCalculated = { total -> onTotalPagesCalculated(total) },
            onPageChanged = { current, _ -> onCurrentPageChanged(current) },
            onProgressChanged = { readingProgress, chapterId, documentPositionData ->
                onProgressChanged(
                    readingProgress,
                    chapterId ?: "",
                    documentPositionData
                )
            },
        )
    }

    LaunchedEffect(bridge) {
        goToPage.collect { page ->
            bridge.goToPage(page)
        }
    }

    EpubWebViewBase(
        book = book,
        chapters = chapters,
        assetFileName = "epub/epub_paged_template.html",
        extraJavascriptInterfaces = listOf(bridge to "NativeApp"),
        onScreenTapped = onScreenTapped,
        modifier = modifier,
    )
}
