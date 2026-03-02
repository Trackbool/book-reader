package com.trackbool.bookreader.domain.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType

interface BookMetadataParserFactory {
    fun getParser(fileType: BookFileType): BookMetadataParser?
}
