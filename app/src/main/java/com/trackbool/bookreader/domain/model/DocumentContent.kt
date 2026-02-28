package com.trackbool.bookreader.domain.model

data class ChapterMetadata(
    val title: String?,
    val chapterIndex: Int
)

data class Chapter(
    val metadata: ChapterMetadata,
    val content: String
)

data class DocumentContent(
    val chapters: List<ChapterMetadata>,
    val language: String? = null
) {
    val totalChapters: Int
        get() = chapters.size
}
