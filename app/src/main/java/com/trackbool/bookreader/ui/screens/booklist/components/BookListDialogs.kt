package com.trackbool.bookreader.ui.screens.booklist.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trackbool.bookreader.R

@Composable
fun DeleteMultipleBooksDialog(
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

@Composable
fun ReaderModeDialog(
    bookTitle: String,
    onDismiss: () -> Unit,
    onScrollModeSelected: () -> Unit,
    onPagedModeSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_reading_mode)) },
        text = { Text(bookTitle) },
        confirmButton = {
            TextButton(onClick = onScrollModeSelected) {
                Text(stringResource(R.string.scroll_mode))
            }
        },
        dismissButton = {
            TextButton(onClick = onPagedModeSelected) {
                Text(stringResource(R.string.paged_mode))
            }
        }
    )
}
