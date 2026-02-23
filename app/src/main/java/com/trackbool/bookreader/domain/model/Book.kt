package com.trackbool.bookreader.domain.model

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val description: String = "",
    val coverUrl: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f

    val progressText: String
        get() = "$currentPage/$totalPages"
}
