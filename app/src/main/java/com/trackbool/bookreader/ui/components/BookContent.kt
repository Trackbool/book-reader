package com.trackbool.bookreader.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.components.epub.EpubReaderContent
import com.trackbool.bookreader.ui.model.ChapterView

@Composable
fun BookContent(
    book: Book,
    chapters: List<ChapterView>,
    modifier: Modifier = Modifier,
) {
    when (book.fileType) {
        BookFileType.EPUB -> EpubReaderContent(
            book = book,
            chapters = chapters,
            modifier = modifier,
        )
        BookFileType.PDF -> {
            UnsupportedFormatMessage(format = "PDF", modifier = modifier)
        }
        BookFileType.NONE -> {
            UnsupportedFormatMessage(format = "Unknown", modifier = modifier)
        }
    }
}

@Composable
private fun UnsupportedFormatMessage(
    format: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Format '$format' is not supported.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
