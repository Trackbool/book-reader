package com.trackbool.bookreader.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.usecase.AddBookUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBookUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val getAllBooksUseCase: GetAllBooksUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val importBookUseCase: ImportBookUseCase
) : ViewModel() {

    val books: StateFlow<List<Book>> = getAllBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun addBook(title: String, author: String, totalPages: Int) {
        viewModelScope.launch {
            addBookUseCase(title, author, totalPages)
        }
    }

    fun importBook(uri: Uri, title: String, author: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            val result = importBookUseCase(uri, title, author)
            _importState.value = if (result.isSuccess) {
                ImportState.Success
            } else {
                ImportState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
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

sealed class ImportState {
    data object Idle : ImportState()
    data object Importing : ImportState()
    data object Success : ImportState()
    data class Error(val message: String) : ImportState()
}
