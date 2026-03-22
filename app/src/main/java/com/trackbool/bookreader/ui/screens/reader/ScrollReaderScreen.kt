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
import com.trackbool.bookreader.ui.screens.reader.components.content.BookScrollContent
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.ReaderBottomSheet
import com.trackbool.bookreader.ui.screens.reader.components.ReaderContent
import com.trackbool.bookreader.ui.screens.reader.components.ReaderProgress
import com.trackbool.bookreader.ui.screens.reader.components.ReaderTopBar
import com.trackbool.bookreader.ui.screens.reader.components.ScrollReaderControls
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollReaderScreen(
    book: Book,
    chapters: List<ChapterView>,
    currentChapter: ChapterView?,
    onProgressSelected: (Float) -> Unit,
    goToProgress: SharedFlow<Float>,
    isLoading: Boolean,
    hasError: Boolean,
    readerSettings: ReaderSettings,
    onFontSizeChanged: (Int) -> Unit,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    onBack: () -> Unit,
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
                    BookScrollContent(
                        book = book,
                        chapters = chapters,
                        onContentReady = onContentReady,
                        onProgressChanged = onProgressChanged,
                        goToProgress = goToProgress,
                        onScreenTapped = { controlsVisible = !controlsVisible },
                        readerSettings = readerSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    ScrollReaderControls(
                        progressPercent = book.progressPercent,
                        onProgressChanged = onProgressSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            AnimatedContent(
                targetState = controlsVisible,
                label = "BottomBarTransition",
                modifier = Modifier.fillMaxWidth()
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
