package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.usecase.AddBookUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBookUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookViewModel(
    private val getAllBooksUseCase: GetAllBooksUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val deleteBookUseCase: DeleteBookUseCase
) : ViewModel() {

    val books: StateFlow<List<Book>> = getAllBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBook(title: String, author: String, totalPages: Int) {
        viewModelScope.launch {
            addBookUseCase(title, author, totalPages)
        }
    }

    fun updateProgress(book: Book, currentPage: Int) {
        viewModelScope.launch {
            updateBookProgressUseCase(book, currentPage)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            deleteBookUseCase(book)
        }
    }
}

class BookViewModelFactory(
    private val getAllBooksUseCase: GetAllBooksUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val deleteBookUseCase: DeleteBookUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(
                getAllBooksUseCase,
                addBookUseCase,
                updateBookProgressUseCase,
                deleteBookUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
