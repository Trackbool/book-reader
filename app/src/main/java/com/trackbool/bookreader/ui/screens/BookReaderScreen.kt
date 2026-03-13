package com.trackbool.bookreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    currentChapter: ChapterView?,
    isLoading: Boolean,
    hasError: Boolean,
    currentPage: Int,
    totalPages: Int,
    onBack: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readerMode = ReaderMode.PAGED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentChapter?.title.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                    .fillMaxWidth(),
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
                                onProgressChanged = onProgressChanged,
                                modifier = Modifier.fillMaxSize()
                            )

                            ReaderMode.PAGED -> BookPagedContent(
                                book = book,
                                chapters = chapters,
                                onContentReady = onContentReady,
                                onCurrentPageChanged = onCurrentPageChanged,
                                onTotalPagesCalculated = onTotalPagesCalculated,
                                onProgressChanged = onProgressChanged,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }
            }

            BookProgress(
                book = book,
                currentPage = currentPage,
                totalPages = totalPages,
                readerMode = readerMode,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun BookProgress(
    book: Book,
    currentPage: Int,
    totalPages: Int,
    readerMode: ReaderMode,
    modifier: Modifier = Modifier
) {
    val progressText = when {
        totalPages == 0 -> stringResource(R.string.preparing_your_reading)
        readerMode == ReaderMode.PAGED -> "$currentPage / $totalPages • ${book.progressPercent}%"
        else -> "${book.progressPercent}%"
    }

    Text(
        text = progressText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    )
}