package com.trackbool.bookreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.trackbool.bookreader.ui.screens.booklist.BookListScreen
import com.trackbool.bookreader.ui.screens.booklist.BookListViewModel
import com.trackbool.bookreader.ui.screens.booklist.ReaderMode
import com.trackbool.bookreader.ui.screens.reader.PagedReaderScreen
import com.trackbool.bookreader.ui.screens.reader.ScrollReaderScreen
import com.trackbool.bookreader.ui.screens.reader.PagedReaderViewModel
import com.trackbool.bookreader.ui.screens.reader.ScrollReaderViewModel

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

            BookListScreen(
                viewModel = viewModel,
                onOpenBook = { book, mode ->
                    val route = when (mode) {
                        ReaderMode.SCROLL -> AppScreens.ScrollReaderScreen.createRoute(book.id)
                        ReaderMode.PAGED -> AppScreens.PagedReaderScreen.createRoute(book.id)
                    }
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = AppScreens.ScrollReaderScreen.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) {
            val viewModel: ScrollReaderViewModel = hiltViewModel()
            val book by viewModel.book.collectAsStateWithLifecycle()
            val chapters by viewModel.chapters.collectAsStateWithLifecycle()
            val currentChapter by viewModel.currentChapter.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val hasError by viewModel.hasError.collectAsStateWithLifecycle()
            val goToProgress = viewModel.goToProgress

            book?.let { b ->
                ScrollReaderScreen(
                    book = b,
                    chapters = chapters,
                    currentChapter = currentChapter,
                    onProgressSelected = viewModel::onProgressSelected,
                    goToProgress = goToProgress,
                    isLoading = isLoading,
                    hasError = hasError,
                    onContentReady = viewModel::onContentReady,
                    onProgressChanged = viewModel::onProgressChanged,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = AppScreens.PagedReaderScreen.route,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType }),
        ) {
            val viewModel: PagedReaderViewModel = hiltViewModel()
            val book by viewModel.book.collectAsStateWithLifecycle()
            val chapters by viewModel.chapters.collectAsStateWithLifecycle()
            val currentChapter by viewModel.currentChapter.collectAsStateWithLifecycle()
            val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
            val hasError by viewModel.hasError.collectAsStateWithLifecycle()
            val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
            val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
            val goToPage = viewModel.goToPage

            book?.let { b ->
                PagedReaderScreen(
                    book = b,
                    chapters = chapters,
                    currentChapter = currentChapter,
                    isLoading = isLoading,
                    hasError = hasError,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    goToPage = goToPage,
                    onPageSelected = viewModel::onPageSelected,
                    onContentReady = viewModel::onContentReady,
                    onProgressChanged = viewModel::onProgressChanged,
                    onBack = { navController.popBackStack() },
                    onCurrentPageChanged = viewModel::onPageChanged,
                    onTotalPagesCalculated = viewModel::onTotalPagesCalculated,
                )
            }
        }
    }
}
