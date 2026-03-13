package com.trackbool.bookreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trackbool.bookreader.ui.screens.BookListScreen
import com.trackbool.bookreader.ui.screens.BookReaderScreen
import com.trackbool.bookreader.viewmodel.BookListViewModel
import com.trackbool.bookreader.viewmodel.BookReaderViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = AppScreens.BookListScreen.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = AppScreens.BookListScreen.route) {
            val viewModel: BookListViewModel = hiltViewModel()
            val books by viewModel.books.collectAsState()
            val importState by viewModel.importState.collectAsState()
            val deleteState by viewModel.deleteState.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val isSelectionMode by viewModel.isSelectionMode.collectAsState()
            val selectedBooks by viewModel.selectedBooks.collectAsState()

            BookListScreen(
                books = books,
                onImportBooks = viewModel::importBooks,
                onDeleteBooks = viewModel::deleteBooks,
                onBookClick = { book ->
                    navController.navigate(
                        AppScreens.BookReaderScreen.createRoute(book.id)
                    ) {
                        launchSingleTop = true
                    }
                },
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
                onEnterSelectionMode = viewModel::enterSelectionMode,
            )
        }

        composable(
            route = AppScreens.BookReaderScreen.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) {
            val viewModel: BookReaderViewModel = hiltViewModel()
            val book by viewModel.book.collectAsState()
            val chapters by viewModel.chapters.collectAsState()
            val currentChapter by viewModel.currentChapter.collectAsState()
            val isLoading by viewModel.isLoading.collectAsState()
            val hasError by viewModel.hasError.collectAsState()
            val currentPage by viewModel.currentPage.collectAsState()
            val totalPages by viewModel.totalPages.collectAsState()

            book?.let { b ->
                BookReaderScreen(
                    book = b,
                    chapters = chapters,
                    currentChapter = currentChapter,
                    isLoading = isLoading,
                    hasError = hasError,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onBack = { navController.popBackStack() },
                    onCurrentPageChanged = viewModel::onPageChanged,
                    onTotalPagesCalculated = viewModel::onTotalPagesCalculated,
                    onProgressChanged = viewModel::onProgressChanged,
                    onContentReady = viewModel::onContentReady,
                )
            }
        }
    }
}