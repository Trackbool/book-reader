package com.trackbool.bookreader.ui.screens.reader.components.epub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.common.model.ChapterView
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun EpubScrollReader(
    book: Book,
    chapters: List<ChapterView>,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    goToProgress: SharedFlow<Float>,
    modifier: Modifier = Modifier,
    onScreenTapped: () -> Unit,
) {
    val bridge = remember {
        EpubBridge(
            onContentReady = onContentReady,
            onPagesCalculated = {},
            onPageChanged = { _, _ -> },
            onProgressChanged = { readingProgress, chapterId, documentPositionData ->
                onProgressChanged(
                    readingProgress,
                    chapterId ?: "",
                    documentPositionData
                )
            }
        )
    }

    LaunchedEffect(bridge) {
        goToProgress.collect { progress ->
            bridge.goToProgress(progress)
        }
    }

    EpubWebViewBase(
        book = book,
        chapters = chapters,
        assetFileName = "epub/epub_scroll_template.html",
        extraJavascriptInterfaces = listOf(bridge to "NativeApp"),
        onScreenTapped = onScreenTapped,
        modifier = modifier,
    )
}
