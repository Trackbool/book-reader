package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.ChapterContent
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.domain.usecase.GetBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import com.trackbool.bookreader.ui.model.ChapterView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBookUseCase: GetBookUseCase,
    private val getBookContentUseCase: GetBookContentUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterView>>(emptyList())
    val chapters: StateFlow<List<ChapterView>> = _chapters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    fun onPageChanged(page: Int) {
        _currentPage.value = page
        saveProgress()
    }

    fun onTotalPagesCalculated(total: Int) {
        _totalPages.value = total
    }

    fun onContentReady() {
        _isLoading.value = false
    }

    fun getInitialPage(): Int {
        val progress = _book.value?.readingProgress ?: 0f
        val total = _totalPages.value
        return if (total > 0) (progress * total).toInt() else 0
    }

    private fun saveProgress() {
        val book = _book.value ?: return
        val current = _currentPage.value
        val total = _totalPages.value
        if (total <= 0) return

        viewModelScope.launch {
            updateBookProgressUseCase(book, current, total)
        }
    }

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
            val content = getBookContentUseCase(book)
            _chapters.value = content?.chapters
                ?.mapIndexed { index, chapter ->
                    ChapterView(
                        title = chapter.metadata.title.orEmpty(),
                        id = chapter.metadata.id,
                        content = chapterContentFor(book, chapter.content, index),
                    )
                }
                .orEmpty()
        }
    }

    private fun chapterContentFor(
        book: Book,
        rawContent: String,
        chapterIndex: Int,
    ): ChapterContent = when (book.fileType) {
        BookFileType.EPUB -> ChapterContent.Html(rawContent)
        BookFileType.PDF -> ChapterContent.Pdf(
            filePath = book.filePath,
            pageRange = chapterIndex..chapterIndex,
        )
        BookFileType.NONE -> ChapterContent.Html(rawContent)
    }
}