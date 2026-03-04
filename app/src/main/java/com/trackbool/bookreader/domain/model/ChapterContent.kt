package com.trackbool.bookreader.domain.model

sealed class ChapterContent {
    data class Html(val html: String) : ChapterContent()
    data class Pdf(val filePath: String, val pageRange: IntRange) : ChapterContent()
}