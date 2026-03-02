package com.trackbool.bookreader.domain.parser.text

import com.trackbool.bookreader.domain.model.BookFileType

interface TextParserFactory {
    fun getParser(fileType: BookFileType): TextParser?
}
