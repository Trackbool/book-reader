package com.trackbool.bookreader.ui.model

import androidx.compose.ui.text.AnnotatedString

data class ChapterView(
    val title: String?,
    val items: List<ReaderText>
)

sealed class ReaderText {
    data class Text(
        val annotatedString: AnnotatedString
    ) : ReaderText()

    data class Image(
        val src: String,
        val alt: String? = null
    ) : ReaderText()
}
