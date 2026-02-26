package com.trackbool.bookreader.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.R
import com.trackbool.bookreader.data.source.AndroidBookSource
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.source.BookSource
import com.trackbool.bookreader.domain.usecase.AddBooksUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBooksUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBooksUseCase
import com.trackbool.bookreader.domain.usecase.UpdateBookProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val getAllBooksUseCase: GetAllBooksUseCase,
    private val importBooksUseCase: ImportBooksUseCase,
    private val addBooksUseCase: AddBooksUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val deleteBooksUseCase: DeleteBooksUseCase,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val books: StateFlow<List<Book>> = getAllBooksUseCase()
        .onStart { _isLoading.value = true }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    val supportedMimeTypes: List<String> = listOf("application/pdf", "application/epub+zip")

    private val _selectedBooks = MutableStateFlow<Set<Book>>(emptySet())
    val selectedBooks: StateFlow<Set<Book>> = _selectedBooks.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun toggleBookSelection(book: Book) {
        val current = _selectedBooks.value
        _selectedBooks.value = if (current.contains(book)) current - book else current + book
        updateSelectionMode()
    }

    fun clearSelection() {
        _selectedBooks.value = emptySet()
        _isSelectionMode.value = false
    }

    fun enterSelectionMode(book: Book) {
        _isSelectionMode.value = true
        _selectedBooks.value = setOf(book)
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedBooks.value = emptySet()
    }

    private fun updateSelectionMode() {
        _isSelectionMode.value = _selectedBooks.value.isNotEmpty()
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun importBooks(uris: List<Uri>) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing

            val bookSources = uris.map { uri ->
                AndroidBookSource(context, uri)
            }

            val titles = bookSources.map { bookSource ->
                bookSource.getFileName()?.substringBeforeLast(".")
                    ?: context.getString(R.string.untitled)
            }
            val authors = List(bookSources.size) { "" }

            val importResult = importBooksUseCase(bookSources, titles, authors)

            if (importResult.isSuccess) {
                val books = importResult.getOrThrow()
                val addResult = addBooksUseCase(books)
                _importState.value = if (addResult.isSuccess) {
                    ImportState.Success(books.size)
                } else {
                    ImportState.Error(
                        addResult.exceptionOrNull()?.message
                            ?: context.getString(R.string.error_save_book)
                    )
                }
            } else {
                _importState.value = ImportState.Error(
                    importResult.exceptionOrNull()?.message
                        ?: context.getString(R.string.error_import_book)
                )
            }
        }
    }

    fun updateProgress(book: Book, currentPage: Int) {
        viewModelScope.launch {
            updateBookProgressUseCase(book, currentPage)
        }
    }

    fun deleteBooks(books: List<Book>) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            try {
                deleteBooksUseCase(books)
                _deleteState.value = DeleteState.Success(books.size)
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(
                    e.message ?: context.getString(R.string.error_delete_book),
                    books.size
                )
            }
        }
    }
}

sealed class ImportState {
    data object Idle : ImportState()
    data object Importing : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Deleting : DeleteState()
    data class Success(val count: Int) : DeleteState()
    data class Error(val message: String, val count: Int = 0) : DeleteState()
}
