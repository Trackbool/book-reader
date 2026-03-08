package com.trackbool.bookreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.components.BookPagedContent
import com.trackbool.bookreader.ui.components.BookScrollContent
import com.trackbool.bookreader.ui.components.LoadingIndicator
import com.trackbool.bookreader.ui.model.ChapterView
import com.trackbool.bookreader.ui.model.ReaderMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    book: Book,
    chapters: List<ChapterView>,
    isLoading: Boolean,
    hasError: Boolean,
    currentPage: Int,
    totalPages: Int,
    onBack: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    onContentReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readerMode = ReaderMode.PAGED

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (!isLoading) {
                        BookProgress(
                            book = book,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            readerMode = readerMode,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                hasError || (!isLoading && chapters.isEmpty()) -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.reader_error_loading_content))
                    }
                }

                chapters.isNotEmpty() -> {
                    when (readerMode) {
                        ReaderMode.SCROLL -> BookScrollContent(
                            book = book,
                            chapters = chapters,
                            onContentReady = onContentReady,
                            modifier = Modifier.fillMaxSize(),
                        )

                        ReaderMode.PAGED -> BookPagedContent(
                            book = book,
                            chapters = chapters,
                            onContentReady = onContentReady,
                            onCurrentPageChanged = onCurrentPageChanged,
                            onTotalPagesCalculated = onTotalPagesCalculated,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            if (isLoading) {
                LoadingIndicator(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun BookProgress(book: Book, currentPage: Int, totalPages: Int, readerMode: ReaderMode) {
    val progressText = if (readerMode == ReaderMode.PAGED)
        "$currentPage/$totalPages - ${book.progressPercent}%" else "${book.progressPercent}%"

    Text(
        text = progressText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(end = 16.dp),
    )
}