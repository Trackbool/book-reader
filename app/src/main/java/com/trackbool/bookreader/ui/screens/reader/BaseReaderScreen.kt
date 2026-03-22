package com.trackbool.bookreader.ui.screens.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.trackbool.bookreader.ui.common.model.ChapterView
import com.trackbool.bookreader.ui.screens.reader.components.ReaderBottomSheet
import com.trackbool.bookreader.ui.screens.reader.components.ReaderContent
import com.trackbool.bookreader.ui.screens.reader.components.ReaderProgress
import com.trackbool.bookreader.ui.screens.reader.components.ReaderTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseReaderScreen(
    book: Book,
    chapters: List<ChapterView>,
    currentChapter: ChapterView?,
    isLoading: Boolean,
    hasError: Boolean,
    readerSettings: ReaderSettings,
    showControls: Boolean,
    progressPercent: Float,
    modifier: Modifier = Modifier,
    currentPage: Int? = null,
    totalPages: Int? = null,
    onFontSizeChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onScreenTapped: () -> Unit,
    content: @Composable (onScreenTapped: () -> Unit) -> Unit,
    controls: @Composable () -> Unit,
) {
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
            ReaderContentArea(
                isLoading = isLoading,
                hasError = hasError,
                chapters = chapters,
                showControls = showControls,
                onScreenTapped = onScreenTapped,
                content = content,
                controls = controls,
                modifier = Modifier.weight(1f)
            )

            ReaderBottomBar(
                showControls = showControls,
                progressPercent = progressPercent,
                currentPage = currentPage,
                totalPages = totalPages
            )
        }

        ReaderBottomSheet(
            show = bottomSheetVisible,
            settings = readerSettings,
            onFontSizeChanged = onFontSizeChanged,
            onDismiss = { bottomSheetVisible = false }
        )
    }
}

@Composable
private fun ReaderContentArea(
    isLoading: Boolean,
    hasError: Boolean,
    chapters: List<ChapterView>,
    showControls: Boolean,
    onScreenTapped: () -> Unit,
    content: @Composable (onScreenTapped: () -> Unit) -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        ReaderContent(
            isLoading = isLoading,
            hasError = hasError,
            chapters = chapters,
            modifier = Modifier.fillMaxSize()
        ) {
            content(onScreenTapped)
        }

        ReaderControlsOverlay(
            showControls = showControls,
            controls = controls,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ReaderControlsOverlay(
    showControls: Boolean,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        controls()
    }
}

@Composable
private fun ReaderBottomBar(
    showControls: Boolean,
    progressPercent: Float,
    currentPage: Int?,
    totalPages: Int?,
) {
    AnimatedContent(
        targetState = showControls,
        label = "BottomBarTransition",
        modifier = Modifier.fillMaxWidth()
    ) { showControlsNow ->
        if (showControlsNow) {
            Box(Modifier.fillMaxWidth())
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ReaderProgress(
                    progressPercent = progressPercent,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
