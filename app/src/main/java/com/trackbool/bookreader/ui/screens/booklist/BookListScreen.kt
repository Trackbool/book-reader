package com.trackbool.bookreader.ui.screens.booklist

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.ui.common.components.LoadingIndicator
import com.trackbool.bookreader.ui.common.components.OptionItem
import com.trackbool.bookreader.ui.common.components.OptionsDialog
import com.trackbool.bookreader.ui.screens.booklist.components.BookListContent
import com.trackbool.bookreader.ui.screens.booklist.components.BookListFab
import com.trackbool.bookreader.ui.screens.booklist.components.BookListTopBar
import com.trackbool.bookreader.ui.screens.booklist.components.DeleteMultipleBooksDialog
import com.trackbool.bookreader.ui.screens.booklist.components.ReaderModeDialog

@Composable
fun BookListScreen(
    viewModel: BookListViewModel,
    onOpenBook: (Book, ReaderMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedBooks by viewModel.selectedBooks.collectAsStateWithLifecycle()

    BookListScreen(
        books = books,
        onImportBooks = viewModel::importBooks,
        onDeleteBooks = viewModel::deleteBooks,
        onOpenBook = onOpenBook,
        onBookLongClick = viewModel::enterSelectionMode,
        importState = importState,
        onResetImportState = viewModel::resetImportState,
        deleteState = deleteState,
        onResetDeleteState = viewModel::resetDeleteState,
        isLoading = isLoading,
        supportedMimeTypes = viewModel.supportedMimeTypes,
        isSelectionMode = isSelectionMode,
        selectedBooks = selectedBooks,
        onToggleBookSelection = viewModel::toggleBookSelection,
        onClearSelection = viewModel::clearSelection,
        modifier = modifier
    )
}

@Composable
private fun BookListScreen(
    books: List<Book>,
    onImportBooks: (List<android.net.Uri>) -> Unit,
    onDeleteBooks: (List<Book>) -> Unit,
    onOpenBook: (Book, ReaderMode) -> Unit,
    onBookLongClick: (Book) -> Unit,
    importState: ImportState,
    onResetImportState: () -> Unit,
    deleteState: DeleteState,
    onResetDeleteState: () -> Unit,
    isLoading: Boolean,
    supportedMimeTypes: List<String>,
    isSelectionMode: Boolean,
    selectedBooks: Set<Book>,
    onToggleBookSelection: (Book) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var selectedBookForOptions by remember { mutableStateOf<Book?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var bookToOpen by remember { mutableStateOf<Book?>(null) }

    if (selectedBookForOptions != null && !isSelectionMode) {
        val optionsTitle = "Book options"
        val deleteLabel = "Delete from library"

        OptionsDialog(
            title = optionsTitle,
            options = listOf(
                OptionItem(
                    label = deleteLabel,
                    onClick = { selectedBookForOptions?.let { onDeleteBooks(listOf(it)) } }
                )
            ),
            onDismiss = { selectedBookForOptions = null }
        )
    }

    if (showDeleteDialog && selectedBooks.isNotEmpty()) {
        DeleteMultipleBooksDialog(
            bookCount = selectedBooks.size,
            onConfirm = {
                onDeleteBooks(selectedBooks.toList())
                onClearSelection()
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    bookToOpen?.let { book ->
        ReaderModeDialog(
            bookTitle = book.title,
            onDismiss = { bookToOpen = null },
            onScrollModeSelected = { onOpenBook(book, ReaderMode.SCROLL) },
            onPagedModeSelected = { onOpenBook(book, ReaderMode.PAGED) }
        )
    }

    ImportStateEffect(
        importState = importState,
        snackBarHostState = snackBarHostState,
        onResetImportState = onResetImportState
    )

    DeleteStateEffect(
        deleteState = deleteState,
        snackBarHostState = snackBarHostState,
        onResetDeleteState = onResetDeleteState
    )

    Scaffold(
        topBar = {
            BookListTopBar(
                isSelectionMode = isSelectionMode,
                selectedCount = selectedBooks.size,
                onClearSelection = onClearSelection,
                onDeleteClick = { showDeleteDialog = true }
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                BookListFab(
                    supportedMimeTypes = supportedMimeTypes,
                    onImportBooks = onImportBooks
                )
            }
        },
        snackbarHost = { SnackbarHost(snackBarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (isLoading) {
            LoadingIndicator()
        } else {
            BookListContent(
                books = books,
                paddingValues = paddingValues,
                onBookMoreClick = { selectedBookForOptions = it },
                onBookLongClick = onBookLongClick,
                onBookClick = { book ->
                    if (isSelectionMode) {
                        onToggleBookSelection(book)
                    } else {
                        bookToOpen = book
                    }
                },
                isSelectionMode = isSelectionMode,
                selectedBooks = selectedBooks
            )
        }
    }
}

@Composable
private fun ImportStateEffect(
    importState: ImportState,
    snackBarHostState: SnackbarHostState,
    onResetImportState: () -> Unit
) {
    val importSuccessMessage = "Book imported successfully"
    val importSuccessMessagePlural = "%d books imported successfully"
    val importErrorMessage = "Failed to import book"
    val saveErrorMessage = "Failed to save book"

    LaunchedEffect(importState) {
        when (importState) {
            is ImportState.Success -> {
                val message = if (importState.count > 1) {
                    String.format(importSuccessMessagePlural, importState.count)
                } else {
                    importSuccessMessage
                }
                snackBarHostState.showSnackbar(message)
                onResetImportState()
            }
            is ImportState.Error -> {
                val messageResId = importState.messageResId
                val message = if (messageResId == R.string.error_import_book) importErrorMessage else saveErrorMessage
                snackBarHostState.showSnackbar(message)
                onResetImportState()
            }
            else -> {}
        }
    }
}

@Composable
private fun DeleteStateEffect(
    deleteState: DeleteState,
    snackBarHostState: SnackbarHostState,
    onResetDeleteState: () -> Unit
) {
    val deleteSuccessMessage = "Book deleted"
    val deleteSuccessMessagePlural = "%d books deleted"
    val deleteErrorMessage = "Failed to delete book"

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                val message = if (deleteState.count > 1) {
                    String.format(deleteSuccessMessagePlural, deleteState.count)
                } else {
                    deleteSuccessMessage
                }
                snackBarHostState.showSnackbar(message)
                onResetDeleteState()
            }
            is DeleteState.Error -> {
                snackBarHostState.showSnackbar(deleteErrorMessage)
                onResetDeleteState()
            }
            else -> {}
        }
    }
}
