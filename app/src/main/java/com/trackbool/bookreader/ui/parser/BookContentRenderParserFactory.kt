package com.trackbool.bookreader.ui.parser

import com.trackbool.bookreader.domain.model.BookFileType

interface BookContentRenderParserFactory {
    fun getParser(fileType: BookFileType): BookContentRenderParser?
}
