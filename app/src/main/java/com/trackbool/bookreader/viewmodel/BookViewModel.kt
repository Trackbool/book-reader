package com.trackbool.bookreader.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.data.source.AndroidBookSource
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.usecase.AddBookUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBookUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBookUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getAllBooksUseCase: GetAllBooksUseCase,
    private val importBookUseCase: ImportBookUseCase,
    private val addBookUseCase: AddBookUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
) : ViewModel() {

    val books: StateFlow<List<Book>> = getAllBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun importBook(uri: Uri, title: String, author: String) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            val bookSource = AndroidBookSource(context, uri)
            val importResult = importBookUseCase(bookSource, title, author)
            
            if (importResult.isSuccess) {
                val book = importResult.getOrThrow()
                addBookUseCase(book)
                _importState.value = ImportState.Success
            } else {
                _importState.value = ImportState.Error(importResult.exceptionOrNull()?.message ?: "Error desconocido")
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
