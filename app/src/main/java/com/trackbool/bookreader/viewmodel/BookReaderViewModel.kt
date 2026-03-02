package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
import com.trackbool.bookreader.ui.model.ChapterView
import com.trackbool.bookreader.ui.parser.BookContentRenderParserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val getBookContentUseCase: GetBookContentUseCase,
    private val bookContentRenderParserFactory: BookContentRenderParserFactory
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterView>?>(null)
    val chapters: StateFlow<List<ChapterView>?> = _chapters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _isLoading.value = true
            val bookResult = bookRepository.getBookById(bookId)
            bookResult.onSuccess { book ->
                _book.value = book
                loadContent(book)
            }.onFailure {
                _isLoading.value = false
            }
        }
    }

    private fun loadContent(book: Book) {
        viewModelScope.launch {
            val contentResult = getBookContentUseCase(book)
            val contentParser = bookContentRenderParserFactory.getParser(book.fileType)
            
            val parsedChapters = contentResult?.chapters?.map { chapter ->
                ChapterView(
                    title = chapter.metadata.title,
                    items = contentParser?.parse(chapter.content) ?: emptyList()
                )
            }
            _chapters.value = parsedChapters
            
            _isLoading.value = false
        }
    }
}
