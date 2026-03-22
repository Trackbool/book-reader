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
import com.trackbool.bookreader.ui.screens.reader.components.ScrollReaderControls
import com.trackbool.bookreader.ui.screens.reader.components.content.BookScrollContent
import com.trackbool.bookreader.ui.common.model.ChapterView
import kotlinx.coroutines.flow.SharedFlow

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

    BaseReaderScreen(
        book = book,
        chapters = chapters,
        currentChapter = currentChapter,
        isLoading = isLoading,
        hasError = hasError,
        readerSettings = readerSettings,
        showControls = controlsVisible,
        progressPercent = book.progressPercent,
        onFontSizeChanged = onFontSizeChanged,
        onBack = onBack,
        onScreenTapped = { controlsVisible = !controlsVisible },
        content = { onTap ->
            BookScrollContent(
                book = book,
                chapters = chapters,
                onContentReady = onContentReady,
                onProgressChanged = onProgressChanged,
                goToProgress = goToProgress,
                onScreenTapped = onTap,
                readerSettings = readerSettings,
                modifier = Modifier.fillMaxSize()
            )
        },
        controls = {
            ScrollReaderControls(
                progressPercent = book.progressPercent,
                onProgressChanged = onProgressSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        },
        modifier = modifier
    )
}
