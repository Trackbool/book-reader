package com.trackbool.bookreader.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trackbool.bookreader.data.epub.EpubAssetResolver
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.ui.components.epub.EpubReaderContent
import com.trackbool.bookreader.ui.model.ChapterView
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun BookContent(
    book: Book,
    chapters: List<ChapterView>,
    modifier: Modifier = Modifier,
) {
    when (book.fileType) {
        BookFileType.EPUB -> {
            val assetResolver = rememberEpubAssetResolver()
            EpubReaderContent(
                chapters = chapters,
                assetResolver = assetResolver,
                modifier = modifier,
            )
        }
        BookFileType.PDF -> {
            // TODO: PdfReaderContent(book, chapters, modifier)
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

@Composable
private fun rememberEpubAssetResolver(): EpubAssetResolver {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors
            .fromApplication(
                context.applicationContext,
                EpubAssetResolverEntryPoint::class.java)
            .epubAssetResolver()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface EpubAssetResolverEntryPoint {
    fun epubAssetResolver(): EpubAssetResolver
}