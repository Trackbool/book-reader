package com.trackbool.bookreader.ui.screens.booklist

import com.trackbool.bookreader.R

sealed class ImportState {
    data object Idle : ImportState()
    data object Importing : ImportState()
    data class Success(val count: Int) : ImportState()
    data class Error(val messageResId: Int) : ImportState()
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Deleting : DeleteState()
    data class Success(val count: Int) : DeleteState()
    data class Error(val messageResId: Int, val count: Int = 0) : DeleteState()
}

enum class ReaderMode {
    SCROLL,
    PAGED
}
