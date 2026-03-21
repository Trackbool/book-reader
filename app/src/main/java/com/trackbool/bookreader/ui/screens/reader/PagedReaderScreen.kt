package com.trackbool.bookreader.ui.screens.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.screens.reader.components.content.BookPagedContent
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.ReaderBottomBar
import com.trackbool.bookreader.ui.screens.reader.components.ReaderContent
import com.trackbool.bookreader.ui.screens.reader.components.ReaderProgress
import com.trackbool.bookreader.ui.screens.reader.components.ReaderTopBar
import com.trackbool.bookreader.ui.screens.reader.components.PagedReaderControls
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
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
    onPageSelected: (Int) -> Unit,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    onBack: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var controlsVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ReaderTopBar(
                book = book,
                currentChapter = currentChapter,
                onBack = onBack,
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ReaderContent(
                isLoading = isLoading,
                hasError = hasError,
                chapters = chapters,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                BookPagedContent(
                    book = book,
                    chapters = chapters,
                    onContentReady = onContentReady,
                    onCurrentPageChanged = onCurrentPageChanged,
                    onTotalPagesCalculated = onTotalPagesCalculated,
                    goToPage = goToPage,
                    onProgressChanged = onProgressChanged,
                    onScreenTapped = { controlsVisible = !controlsVisible },
                    modifier = Modifier.fillMaxSize()
                )
            }

            ReaderBottomBar(
                controlsVisible = controlsVisible,
                showControls = totalPages > 0,
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
                progress = {
                    ReaderProgress(
                        progressPercent = book.progressPercent,
                        currentPage = currentPage,
                        totalPages = totalPages,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
