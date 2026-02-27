package com.trackbool.bookreader.domain.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType

interface DocumentMetadataParserFactory {
    fun getParser(fileType: BookFileType): DocumentMetadataParser?
}
