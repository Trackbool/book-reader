package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.repository.BookRepository
import com.trackbool.bookreader.domain.usecase.GetBookContentUseCase
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
    private val getBookContentUseCase: GetBookContentUseCase
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _content = MutableStateFlow<BookContent?>(null)
    val content: StateFlow<BookContent?> = _content.asStateFlow()

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
            _content.value = contentResult
            _isLoading.value = false
        }
    }
}
