package com.trackbool.bookreader.domain.model

enum class BookFileType {
    PDF,
    EPUB,
    NONE;

    val extension: String
        get() = when (this) {
            PDF -> "pdf"
            EPUB -> "epub"
            NONE -> ""
        }

    val mimeType: String
        get() = when (this) {
            PDF -> "application/pdf"
            EPUB -> "application/epub+zip"
            NONE -> ""
        }

    companion object {
        fun fromExtension(extension: String): BookFileType {
            return when (extension.lowercase()) {
                "pdf" -> PDF
                "epub" -> EPUB
                else -> NONE
            }
        }

        fun fromMimeType(mimeType: String): BookFileType {
            return when (mimeType.lowercase()) {
                "application/pdf" -> PDF
                "application/epub+zip" -> EPUB
                else -> NONE
            }
        }
    }
}

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val description: String = "",
    val coverPath: String = "",
    val readingProgress: Float = 0f,
    val isCompleted: Boolean = false,
    val filePath: String = "",
    val fileType: BookFileType = BookFileType.NONE,
    val fileName: String = ""
) {
    val progressPercent: Int
        get() = (readingProgress * 100).toInt()

    val fileTypeIcon: String
        get() = when (fileType) {
            BookFileType.PDF -> "📄"
            BookFileType.EPUB -> "📚"
            BookFileType.NONE -> "📖"
        }
}
