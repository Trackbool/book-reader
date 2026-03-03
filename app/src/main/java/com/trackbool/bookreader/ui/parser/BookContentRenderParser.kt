package com.trackbool.bookreader.ui.parser

import com.trackbool.bookreader.ui.model.ReaderContent

interface BookContentRenderParser {
    suspend fun parse(text: String): List<ReaderContent>
}
