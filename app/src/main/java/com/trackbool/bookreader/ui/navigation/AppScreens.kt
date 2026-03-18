package com.trackbool.bookreader.ui.navigation

sealed class AppScreens(val route: String) {
    object BookListScreen : AppScreens("book_list")
    object ScrollReaderScreen : AppScreens("scroll_reader/{bookId}") {
        fun createRoute(bookId: Long) = "scroll_reader/$bookId"
    }
    object PagedReaderScreen : AppScreens("paged_reader/{bookId}") {
        fun createRoute(bookId: Long) = "paged_reader/$bookId"
    }
}
