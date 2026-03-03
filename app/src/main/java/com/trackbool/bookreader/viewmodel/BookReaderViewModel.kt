package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.domain.usecase.GetChapterCountUseCase
import com.trackbool.bookreader.domain.usecase.GetChapterUseCase
import com.trackbool.bookreader.ui.model.ChapterView
import com.trackbool.bookreader.ui.parser.BookContentRenderParserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHAPTERS_PER_BATCH = 5

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBookUseCase: GetBookUseCase,
    private val getChapterUseCase: GetChapterUseCase,
    private val getChapterCountUseCase: GetChapterCountUseCase,
    private val bookContentRenderParserFactory: BookContentRenderParserFactory
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterView>>(emptyList())
    val chapters: StateFlow<List<ChapterView>> = _chapters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMoreChapters = MutableStateFlow(true)
    val hasMoreChapters: StateFlow<Boolean> = _hasMoreChapters.asStateFlow()

    private var totalChapters: Int = 0
    private var currentChapterIndex: Int = 0
    private var isLoadingChapters = false

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _isLoading.value = true
            getBookUseCase(bookId).collectLatest { book ->
                _book.value = book
                loadContent(book)
            }
        }
    }

    private fun loadContent(book: Book) {
        viewModelScope.launch {
            totalChapters = getChapterCountUseCase(book)
            loadNextChapters(book)
        }
    }

    fun loadNextChapters(book: Book? = null) {
        if (isLoadingChapters) return
        val currentBook = book ?: _book.value ?: return
        val chaptersToLoad = minOf(CHAPTERS_PER_BATCH, totalChapters - currentChapterIndex)

        if (chaptersToLoad <= 0) {
            _hasMoreChapters.value = false
            _isLoading.value = false
            return
        }

        isLoadingChapters = true
        viewModelScope.launch {
            _isLoading.value = true

            val contentParser = bookContentRenderParserFactory.getParser(currentBook.fileType)

            val indicesToLoad = (currentChapterIndex until currentChapterIndex + chaptersToLoad).toList()
            currentChapterIndex += chaptersToLoad

            val newChapters = indicesToLoad
                .map { index ->
                    async {
                        val chapter = getChapterUseCase(currentBook, index) ?: return@async null
                        val parsedItems = contentParser?.parse(chapter.content) ?: emptyList()
                        ChapterView(
                            id = "${currentBook.id}_$index",
                            title = chapter.metadata.title,
                            items = parsedItems
                        )
                    }
                }
                .awaitAll()
                .filterNotNull()

            _chapters.update { current -> current + newChapters }

            if (currentChapterIndex >= totalChapters) {
                _hasMoreChapters.value = false
            }

            _isLoading.value = false
            isLoadingChapters = false
        }
    }
}