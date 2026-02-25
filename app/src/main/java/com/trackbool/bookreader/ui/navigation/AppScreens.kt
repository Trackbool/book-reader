package com.trackbool.bookreader.ui.navigation

sealed class AppScreens(val route: String) {
    object BookListScreen: AppScreens("book_list_screen")
}