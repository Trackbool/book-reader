package com.trackbool.bookreader.ui.screens.booklist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trackbool.bookreader.R
import com.trackbool.bookreader.domain.model.Book

@Composable
fun BookListContent(
    books: List<Book>,
    paddingValues: PaddingValues,
    onBookMoreClick: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    isSelectionMode: Boolean,
    selectedBooks: Set<Book>,
    modifier: Modifier = Modifier
) {
    if (books.isEmpty()) {
        BookListEmptyState(
            paddingValues = paddingValues,
            modifier = modifier
        )
    } else {
        BooksGrid(
            books = books,
            paddingValues = paddingValues,
            onBookMoreClick = onBookMoreClick,
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            isSelectionMode = isSelectionMode,
            selectedBooks = selectedBooks,
            modifier = modifier
        )
    }
}

@Composable
private fun BookListEmptyState(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
    selectedBooks: Set<Book>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(6.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book) },
                onMoreClick = { onBookMoreClick(book) },
                onLongClick = { onBookLongClick(book) },
                isSelected = selectedBooks.contains(book),
                isSelectionMode = isSelectionMode,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
