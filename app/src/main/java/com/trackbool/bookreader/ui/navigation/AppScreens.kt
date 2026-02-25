package com.trackbool.bookreader.ui.navigation

sealed class AppScreens(val route: String) {
    object BookListScreen : AppScreens("book_list")
    object BookReaderScreen : AppScreens("book_reader/{bookId}") {
        fun createRoute(bookId: Long) = "book_reader/$bookId"
    }
}