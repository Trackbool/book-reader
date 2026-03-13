package com.trackbool.bookreader.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.components.epub.EpubScrollReader
import com.trackbool.bookreader.ui.model.ChapterView

@Composable
fun BookScrollContent(
    book: Book,
    chapters: List<ChapterView>,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (book.fileType) {
        BookFileType.EPUB -> EpubScrollReader(
            book = book,
            chapters = chapters,
            onContentReady = onContentReady,
            onProgressChanged = onProgressChanged,
            modifier = modifier
        )
        BookFileType.PDF -> UnsupportedFormatMessage(format = "PDF", modifier = modifier)
        BookFileType.NONE -> UnsupportedFormatMessage(format = "Unknown", modifier = modifier)
    }
}
