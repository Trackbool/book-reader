package com.trackbool.bookreader.domain.parser.text

import androidx.compose.ui.text.AnnotatedString

sealed class ReaderText {
    data class Text(
        val annotatedString: AnnotatedString
    ) : ReaderText()

    data class Image(
        val src: String,
        val alt: String? = null
    ) : ReaderText()
}
