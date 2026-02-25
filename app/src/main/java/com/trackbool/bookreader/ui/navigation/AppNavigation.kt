package com.trackbool.bookreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trackbool.bookreader.ui.screens.BookListScreen
import com.trackbool.bookreader.viewmodel.BookViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppScreens.BookListScreen.route) {
        composable(route = AppScreens.BookListScreen.route) {

            val viewModel: BookViewModel = hiltViewModel()
            val books by viewModel.books.collectAsState()
            val importState by viewModel.importState.collectAsState()
            BookListScreen(
                navController = navController,
                books = books,
                onImportBook = { bookSource ->
                    viewModel.importBook(bookSource)
                },
                onDeleteBook = { book ->
                    viewModel.deleteBook(book)
                },
                importState = importState,
                onResetImportState = { viewModel.resetImportState() },
                supportedMimeTypes = viewModel.supportedMimeTypes
            )

        }
    }
}