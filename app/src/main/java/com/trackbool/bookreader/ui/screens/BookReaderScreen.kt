package com.trackbool.bookreader.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
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
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.roundToInt

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
    goToPage: SharedFlow<Int>,
    onRequestPage: (Int) -> Unit,
    onBack: () -> Unit,
    onCurrentPageChanged: (Int) -> Unit,
    onTotalPagesCalculated: (Int) -> Unit,
    onContentReady: () -> Unit,
    onProgressChanged: (Float, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readerMode = ReaderMode.PAGED
    var controlsVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = currentChapter?.title.isNullOrBlank(),
                        label = "HeaderTransition"
                    ) { isTitleOnly ->
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = book.title,
                                style = if (isTitleOnly) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!isTitleOnly) {
                                Text(
                                    text = currentChapter?.title ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                                goToPage = goToPage,
                                onProgressChanged = onProgressChanged,
                                onScreenTapped = { controlsVisible = !controlsVisible },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }
            }

            AnimatedContent(
                targetState = controlsVisible && totalPages > 0,
                label = "BottomBarTransition",
                modifier = Modifier.fillMaxWidth()
            ) { showControls ->
                if (showControls) {
                    ReaderControls(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageSelected = onRequestPage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        BookProgress(
                            book = book,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            readerMode = readerMode,
                            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
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

@Composable
fun ReaderControls(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReaderSlider(
                currentPage = currentPage,
                totalPages = totalPages,
                onPageSelected = onPageSelected
            )
        }
    }
}

@Composable
fun ReaderSlider(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember(currentPage) {
        mutableFloatStateOf(currentPage.toFloat())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                R.string.reader_slider_page_indicator,
                sliderPosition.roundToInt(),
                totalPages
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = sliderPosition,
            valueRange = 1f..totalPages.coerceAtLeast(1).toFloat(),
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onPageSelected(sliderPosition.roundToInt()) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}