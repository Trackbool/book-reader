package com.trackbool.bookreader.ui.components.epub

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.components.LoadingIndicator
import com.trackbool.bookreader.ui.model.ChapterView

@Composable
internal fun EpubScrollReader(
    book: Book,
    chapters: List<ChapterView>,
    modifier: Modifier = Modifier,
) {
    EpubWebViewBase(
        book = book,
        chapters = chapters,
        assetFileName = "epub_scroll_template.html",
        modifier = modifier,
        overlayContent = { contentInjected ->
            if (!contentInjected) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                )
            }
        },
    )
}