package com.trackbool.bookreader.ui.screens.reader.components.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.common.components.UnsupportedFormatMessage
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.epub.EpubScrollReader
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun BookScrollContent(
    book: Book,
    chapters: List<ChapterView>,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    goToProgress: SharedFlow<Float>,
    modifier: Modifier = Modifier,
    onScreenTapped: () -> Unit = {},
) {
    when (book.fileType) {
        BookFileType.EPUB -> EpubScrollReader(
            book = book,
            chapters = chapters,
            onContentReady = onContentReady,
            onProgressChanged = onProgressChanged,
            goToProgress = goToProgress,
            onScreenTapped = onScreenTapped,
            modifier = modifier
        )
        BookFileType.PDF -> UnsupportedFormatMessage(format = "PDF", modifier = modifier)
        BookFileType.NONE -> UnsupportedFormatMessage(format = "Unknown", modifier = modifier)
    }
}
