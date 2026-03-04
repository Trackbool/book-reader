package com.trackbool.bookreader.domain.model

data class ChapterMetadata(
    val title: String?,
    val reference: String,
    val chapterIndex: Int
)

data class Chapter(
    val metadata: ChapterMetadata,
    val content: String
)

data class BookContent(
    val chapters: List<Chapter>,
    val language: String? = null
) {
    val totalChapters: Int
        get() = chapters.size
}
