package com.trackbool.bookreader.ui.screens.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.ReaderSettings
import com.trackbool.bookreader.ui.screens.reader.components.content.BookPagedContent
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.ReaderContent
import com.trackbool.bookreader.ui.screens.reader.components.ReaderProgress
import com.trackbool.bookreader.ui.screens.reader.components.ReaderTopBar
import com.trackbool.bookreader.ui.screens.reader.components.PagedReaderControls
import com.trackbool.bookreader.ui.screens.reader.components.ReaderBottomSheet
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
    var bottomSheetVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ReaderTopBar(
                book = book,
                currentChapter = currentChapter,
                onBack = onBack,
                onSettingsClick = { bottomSheetVisible = !bottomSheetVisible }
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ReaderContent(
                    isLoading = isLoading,
                    hasError = hasError,
                    chapters = chapters,
                    modifier = Modifier.fillMaxSize()
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
                        readerSettings = readerSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = controlsVisible && totalPages > 0,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    PagedReaderControls(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageSelected = onPageSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = controlsVisible && totalPages > 0,
                label = "BottomBarTransition",
                modifier = Modifier
                    .fillMaxWidth()
            ) { showControls ->
                if (showControls) {
                    Box(Modifier.fillMaxWidth())
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ReaderProgress(
                            progressPercent = book.progressPercent,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        ReaderBottomSheet(
            show = bottomSheetVisible,
            settings = readerSettings,
            onFontSizeChanged = onFontSizeChanged,
            onDismiss = { bottomSheetVisible = false }
        )
    }
}
