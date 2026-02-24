package com.trackbool.bookreader.ui

import android.net.Uri
import android.provider.OpenableColumns
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
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.viewmodel.ImportState

@Composable
fun BookListScreen(
    books: List<Book>,
    onImportBook: (Uri, String, String) -> Unit,
    importState: ImportState,
    onResetImportState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val importSuccessMessage = stringResource(R.string.import_success)
    val untitled = stringResource(R.string.untitled)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getDisplayName(context, it)
            val title = fileName?.substringBeforeLast(".") ?: untitled
            val author = ""
            onImportBook(it, title, author)
        }
    }

    ImportStateEffect(
        importState = importState,
        snackbarHostState = snackbarHostState,
        importSuccessMessage = importSuccessMessage,
        onResetImportState = onResetImportState
    )

    Scaffold(
        topBar = { BookListTopBar() },
        floatingActionButton = {
            BookListFab(
                onLaunchFilePicker = {
                    filePickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip"))
                }
            )
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
    onLaunchFilePicker: () -> Unit
) {
    FloatingActionButton(onClick = onLaunchFilePicker) {
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

private fun getDisplayName(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}