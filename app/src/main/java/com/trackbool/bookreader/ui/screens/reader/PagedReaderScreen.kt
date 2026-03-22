package com.trackbool.bookreader.ui.screens.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.ReaderSettings
import com.trackbool.bookreader.ui.screens.reader.components.PagedReaderControls
import com.trackbool.bookreader.ui.screens.reader.components.content.BookPagedContent
import com.trackbool.bookreader.ui.common.model.ChapterView
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun PagedReaderScreen(
    book: Book,
    chapters: List<ChapterView>,
    currentChapter: ChapterView?,
    isLoading: Boolean,
    hasError: Boolean,
    currentPage: Int,
    totalPages: Int,
    goToPage: SharedFlow<Int>,
    readerSettings: ReaderSettings,
    onFontSizeChanged: (Int) -> Unit,
    onPageSelected: (Int) -> Unit,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    onBack: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(false) }

    BaseReaderScreen(
        book = book,
        chapters = chapters,
        currentChapter = currentChapter,
        isLoading = isLoading,
        hasError = hasError,
        readerSettings = readerSettings,
        showControls = controlsVisible && totalPages > 0,
        progressPercent = book.progressPercent,
        currentPage = currentPage,
        totalPages = totalPages,
        onFontSizeChanged = onFontSizeChanged,
        onBack = onBack,
        onScreenTapped = { controlsVisible = !controlsVisible },
        content = { onTap ->
            BookPagedContent(
                book = book,
                chapters = chapters,
                onContentReady = onContentReady,
                onCurrentPageChanged = onCurrentPageChanged,
                onTotalPagesCalculated = onTotalPagesCalculated,
                goToPage = goToPage,
                onProgressChanged = onProgressChanged,
                onScreenTapped = onTap,
                readerSettings = readerSettings,
                modifier = Modifier.fillMaxSize()
            )
        },
        controls = {
            PagedReaderControls(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageSelected = onPageSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        },
        modifier = modifier
    )
}
