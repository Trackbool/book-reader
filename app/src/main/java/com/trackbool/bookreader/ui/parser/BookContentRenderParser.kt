package com.trackbool.bookreader.ui.parser

import com.trackbool.bookreader.ui.model.ReaderText

interface BookContentRenderParser {
    suspend fun parse(text: String): List<ReaderText>
}
