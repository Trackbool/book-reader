package com.trackbool.bookreader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.source.BookSource
import com.trackbool.bookreader.domain.usecase.AddBooksUseCase
import com.trackbool.bookreader.domain.usecase.DeleteBookUseCase
import com.trackbool.bookreader.domain.usecase.GetAllBooksUseCase
import com.trackbool.bookreader.domain.usecase.ImportBooksUseCase
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
    private val importBooksUseCase: ImportBooksUseCase,
    private val addBooksUseCase: AddBooksUseCase,
    private val updateBookProgressUseCase: UpdateBookProgressUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
) : ViewModel() {

    val books: StateFlow<List<Book>> = getAllBooksUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    val supportedMimeTypes: List<String> = listOf("application/pdf", "application/epub+zip")

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun importBooks(bookSources: List<BookSource>) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing

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

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            deleteBookUseCase(book)
        }
    }
}

sealed class ImportState {
    data object Idle : ImportState()
    data object Importing : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val message: String) : ImportState()
}
