package com.trackbool.bookreader.ui.screens.reader.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trackbool.bookreader.ui.common.components.LoadingIndicator
import com.trackbool.bookreader.ui.common.model.ChapterView

@Composable
fun ReaderContent(
    isLoading: Boolean,
    hasError: Boolean,
    chapters: List<ChapterView>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            hasError || (!isLoading && chapters.isEmpty()) -> {
                ReaderError(modifier = Modifier.fillMaxSize())
            }

            chapters.isNotEmpty() -> {
                content()
            }
        }

        if (isLoading) {
            LoadingIndicator(modifier = Modifier.fillMaxSize())
        }
    }
}
