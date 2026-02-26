package com.trackbool.bookreader.domain.parser

import com.trackbool.bookreader.domain.model.BookFileType

interface DocumentParserFactory {
    fun getParser(fileType: BookFileType): DocumentParser?
}
