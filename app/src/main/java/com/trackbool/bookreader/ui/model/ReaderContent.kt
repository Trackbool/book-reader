package com.trackbool.bookreader.ui.model

import androidx.compose.ui.text.AnnotatedString

data class ChapterView(
    val id: String,
    val title: String?,
    val items: List<ReaderContent>
)

sealed class ReaderContent {
    data class Text(
        val annotatedString: AnnotatedString
    ) : ReaderContent()

    data class Image(
        val src: String,
        val alt: String? = null
    ) : ReaderContent()
}
