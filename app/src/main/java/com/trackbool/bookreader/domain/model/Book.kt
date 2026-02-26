package com.trackbool.bookreader.domain.model

enum class BookFileType {
    PDF,
    EPUB,
    NONE
}

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val description: String = "",
    val coverPath: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false,
    val filePath: String = "",
    val fileType: BookFileType = BookFileType.NONE,
    val fileName: String = ""
) {
    val progress: Float
        get() = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f

    val progressText: String
        get() = "$currentPage/$totalPages"

    val fileTypeIcon: String
        get() = when (fileType) {
            BookFileType.PDF -> "📄"
            BookFileType.EPUB -> "📚"
            BookFileType.NONE -> "📖"
        }
}
