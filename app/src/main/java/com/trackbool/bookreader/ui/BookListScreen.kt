package com.trackbool.bookreader.ui

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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.trackbool.bookreader.viewmodel.ImportState

@Composable
fun BookListScreen(
    books: List<Book>,
    onImportBook: (BookSource) -> Unit,
    importState: ImportState,
    onResetImportState: () -> Unit,
    supportedMimeTypes: List<String>,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val importSuccessMessage = stringResource(R.string.import_success)
    ImportStateEffect(
        importState = importState,
        snackbarHostState = snackbarHostState,
        importSuccessMessage = importSuccessMessage,
        onResetImportState = onResetImportState
    )

    Scaffold(
        topBar = { BookListTopBar() },
        floatingActionButton = {
            BookListFab(supportedMimeTypes, onImportBook)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        BookListContent(
            books = books,
            paddingValues = paddingValues
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookListTopBar() {
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

@Composable
private fun BookListFab(
    supportedMimeTypes: List<String>,
    onImportBook: (BookSource) -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val bookSource = AndroidBookSource(context, it)
            onImportBook(bookSource)
        }
    }

    FloatingActionButton(onClick = {
        filePickerLauncher.launch(supportedMimeTypes.toTypedArray())
    }) {
        Text("+")
    }
}

@Composable
private fun ImportStateEffect(
    importState: ImportState,
    snackbarHostState: SnackbarHostState,
    importSuccessMessage: String,
    onResetImportState: () -> Unit
) {
    LaunchedEffect(importState) {
        when (importState) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar(importSuccessMessage)
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
private fun BookListContent(
    books: List<Book>,
    paddingValues: PaddingValues
) {
    if (books.isEmpty()) {
        EmptyBooksMessage(paddingValues)
    } else {
        BooksGrid(books, paddingValues)
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
    paddingValues: PaddingValues
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
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
            )
        }
    }
}