package com.trackbool.bookreader.domain.parser.content

import com.trackbool.bookreader.domain.model.BookFileType

interface BookContentParserFactory {
    fun getParser(fileType: BookFileType): DocumentContentParser?
}
