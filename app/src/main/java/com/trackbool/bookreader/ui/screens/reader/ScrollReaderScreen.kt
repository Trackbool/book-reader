package com.trackbool.bookreader.ui.screens.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.screens.reader.components.content.BookScrollContent
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.ReaderBottomBar
import com.trackbool.bookreader.ui.screens.reader.components.ReaderContent
import com.trackbool.bookreader.ui.screens.reader.components.ReaderProgress
import com.trackbool.bookreader.ui.screens.reader.components.ReaderTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollReaderScreen(
    book: Book,
    chapters: List<ChapterView>,
    currentChapter: ChapterView?,
    isLoading: Boolean,
    hasError: Boolean,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                BookScrollContent(
                    book = book,
                    chapters = chapters,
                    onContentReady = onContentReady,
                    onProgressChanged = onProgressChanged,
                    onScreenTapped = { },
                    modifier = Modifier.fillMaxSize()
                )
            }

            ReaderBottomBar(
                controlsVisible = false,
                showControls = false,
                controls = { },
                progress = {
                    ReaderProgress(
                        progressPercent = book.progressPercent,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
