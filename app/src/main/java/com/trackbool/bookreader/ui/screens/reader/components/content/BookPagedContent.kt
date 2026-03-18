package com.trackbool.bookreader.ui.screens.reader.components.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.common.components.UnsupportedFormatMessage
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.epub.EpubPagedReader
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun BookPagedContent(
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
    when (book.fileType) {
        BookFileType.EPUB -> EpubPagedReader(
            book = book,
            chapters = chapters,
            onCurrentPageChanged = onCurrentPageChanged,
            onTotalPagesCalculated = onTotalPagesCalculated,
            goToPage = goToPage,
            onContentReady = onContentReady,
            onProgressChanged = onProgressChanged,
            onScreenTapped = onScreenTapped,
            modifier = modifier,
        )
        BookFileType.PDF -> UnsupportedFormatMessage(format = "PDF", modifier = modifier)
        BookFileType.NONE -> UnsupportedFormatMessage(format = "Unknown", modifier = modifier)
    }
}
