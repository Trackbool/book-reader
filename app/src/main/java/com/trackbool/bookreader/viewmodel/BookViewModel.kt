package com.trackbool.bookreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.data.Book
import com.trackbool.bookreader.data.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookViewModel(private val repository: BookRepository) : ViewModel() {
    val books: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBook(title: String, author: String, totalPages: Int) {
        viewModelScope.launch {
            repository.insertBook(
                Book(
                    title = title,
                    author = author,
                    totalPages = totalPages
                )
            )
        }
    }

    fun updateProgress(book: Book, currentPage: Int) {
        viewModelScope.launch {
            val isCompleted = currentPage >= book.totalPages
            repository.updateBook(book.copy(currentPage = currentPage, isCompleted = isCompleted))
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }
}

class BookViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
