package com.trackbool.bookreader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.R
import com.trackbool.bookreader.data.source.AndroidBookSource
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.source.BookSource
import com.trackbool.bookreader.ui.components.BookCard
import com.trackbool.bookreader.ui.components.LoadingIndicator
import com.trackbool.bookreader.ui.components.OptionItem
import com.trackbool.bookreader.ui.components.OptionsDialog
import com.trackbool.bookreader.viewmodel.DeleteState
import com.trackbool.bookreader.viewmodel.ImportState

@Composable
fun BookListScreen(
    books: List<Book>,
    onImportBooks: (List<BookSource>) -> Unit,
    onDeleteBooks: (List<Book>) -> Unit,
    onBookClick: (Book) -> Unit,
    importState: ImportState,
    onResetImportState: () -> Unit,
    deleteState: DeleteState,
    onResetDeleteState: () -> Unit,
    isLoading: Boolean,
    supportedMimeTypes: List<String>,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedBookState: MutableState<Book?> = remember { mutableStateOf(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedBooks by remember { mutableStateOf(setOf<Book>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun toggleBookSelection(book: Book) {
        selectedBooks = if (selectedBooks.contains(book)) {
            selectedBooks - book
        } else {
            selectedBooks + book
        }
        if (selectedBooks.isEmpty()) {
            isSelectionMode = false
        }
    }

    if (selectedBookState.value != null && !isSelectionMode) {
        val optionsTitle = stringResource(R.string.book_options)
        val deleteLabel = stringResource(R.string.delete_from_library)

        OptionsDialog(
            title = optionsTitle,
            options = listOf(
                OptionItem(
                    label = deleteLabel,
                    onClick = { selectedBookState.value?.let { onDeleteBooks(listOf(it)) } }
                )
            ),
            onDismiss = { selectedBookState.value = null }
        )
    }

    if (showDeleteDialog && selectedBooks.isNotEmpty()) {
        DeleteMultipleBooksDialog(
            bookCount = selectedBooks.size,
            onConfirm = {
                onDeleteBooks(selectedBooks.toList())
                selectedBooks = emptySet()
                isSelectionMode = false
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    val importSuccessMessage = stringResource(R.string.import_success)
    val importSuccessMessagePlural = stringResource(R.string.import_success_plural)
    ImportStateEffect(
        importState = importState,
        snackbarHostState = snackbarHostState,
        importSuccessMessage = importSuccessMessage,
        importSuccessMessagePlural = importSuccessMessagePlural,
        onResetImportState = onResetImportState
    )

    val deleteSuccessMessage = stringResource(R.string.delete_success)
    val deleteSuccessMessagePlural = stringResource(R.string.delete_success_plural)
    val deleteErrorMessage = stringResource(R.string.error_delete_book)
    val deleteErrorMessagePlural = stringResource(R.string.error_delete_book_plural)
    DeleteStateEffect(
        deleteState = deleteState,
        snackbarHostState = snackbarHostState,
        deleteSuccessMessage = deleteSuccessMessage,
        deleteSuccessMessagePlural = deleteSuccessMessagePlural,
        deleteErrorMessage = deleteErrorMessage,
        deleteErrorMessagePlural = deleteErrorMessagePlural,
        onResetDeleteState = onResetDeleteState
    )

    if (isLoading) {
        LoadingIndicator()
    } else {
        Scaffold(
            topBar = {
                BookListTopBar(
                    isSelectionMode = isSelectionMode,
                    selectedCount = selectedBooks.size,
                    onClearSelection = {
                        selectedBooks = emptySet()
                        isSelectionMode = false
                    },
                    onDeleteClick = { showDeleteDialog = true }
                )
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    BookListFab(supportedMimeTypes, onImportBooks)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            modifier = modifier
        ) { paddingValues ->
            BookListContent(
                books = books,
                paddingValues = paddingValues,
                onBookMoreClick = { selectedBookState.value = it },
                onBookLongClick = { book ->
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        selectedBooks = setOf(book)
                    } else {
                        toggleBookSelection(book)
                    }
                },
                onBookClick = { book ->
                    if (isSelectionMode) {
                        toggleBookSelection(book)
                    } else {
                        onBookClick(book)
                    }
                },
                isSelectionMode = isSelectionMode,
                selectedBooks = selectedBooks
            )
        }
    }
}

@Composable
private fun DeleteMultipleBooksDialog(
    bookCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_books_title)) },
        text = {
            Text(
                if (bookCount == 1) {
                    stringResource(R.string.delete_book_message)
                } else {
                    stringResource(R.string.delete_books_message, bookCount)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookListTopBar(
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (isSelectionMode) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "$selectedCount",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                scrolledContainerColor = Color.Unspecified,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    } else {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.my_books),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                scrolledContainerColor = Color.Unspecified,
                navigationIconContentColor = Color.Unspecified,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = Color.Unspecified
            )
        )
    }
}

@Composable
private fun BookListFab(
    supportedMimeTypes: List<String>,
    onImportBooks: (List<BookSource>) -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val bookSources = uris.map { uri ->
                AndroidBookSource(context, uri)
            }
            onImportBooks(bookSources)
        }
    }

    FloatingActionButton(onClick = {
        filePickerLauncher.launch(supportedMimeTypes.toTypedArray())
    }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.import_book))
    }
}

@Composable
private fun ImportStateEffect(
    importState: ImportState,
    snackbarHostState: SnackbarHostState,
    importSuccessMessage: String,
    importSuccessMessagePlural: String,
    onResetImportState: () -> Unit
) {
    LaunchedEffect(importState) {
        when (importState) {
            is ImportState.Success -> {
                val message = if (importState.count > 1) {
                    String.format(importSuccessMessagePlural, importState.count)
                } else {
                    importSuccessMessage
                }
                snackbarHostState.showSnackbar(message)
                onResetImportState()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar(importState.message)
                onResetImportState()
            }
            else -> {}
        }
    }
}

@Composable
private fun DeleteStateEffect(
    deleteState: DeleteState,
    snackbarHostState: SnackbarHostState,
    deleteSuccessMessage: String,
    deleteSuccessMessagePlural: String,
    deleteErrorMessage: String,
    deleteErrorMessagePlural: String,
    onResetDeleteState: () -> Unit
) {
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is DeleteState.Success -> {
                val message = if (deleteState.count > 1) {
                    String.format(deleteSuccessMessagePlural, deleteState.count)
                } else {
                    deleteSuccessMessage
                }
                snackbarHostState.showSnackbar(message)
                onResetDeleteState()
            }
            is DeleteState.Error -> {
                val message = if (deleteState.count > 1) {
                    String.format(deleteErrorMessagePlural, deleteState.count)
                } else {
                    deleteErrorMessage
                }
                snackbarHostState.showSnackbar(message)
                onResetDeleteState()
            }
            else -> {}
        }
    }
}

@Composable
private fun BookListContent(
    books: List<Book>,
    paddingValues: PaddingValues,
    onBookMoreClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    isSelectionMode: Boolean,
    selectedBooks: Set<Book>
) {
    if (books.isEmpty()) {
        EmptyBooksMessage(paddingValues)
    } else {
        BooksGrid(
            books,
            paddingValues,
            onBookMoreClick,
            onBookClick,
            onBookLongClick,
            isSelectionMode,
            selectedBooks
        )
    }
}

@Composable
private fun EmptyBooksMessage(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_books_yet),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BooksGrid(
    books: List<Book>,
    paddingValues: PaddingValues,
    onBookMoreClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    isSelectionMode: Boolean,
    selectedBooks: Set<Book>
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book) },
                onMoreClick = { onBookMoreClick(book) },
                onLongClick = { onBookLongClick(book) },
                isSelected = selectedBooks.contains(book),
                isSelectionMode = isSelectionMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
            )
        }
    }
}
