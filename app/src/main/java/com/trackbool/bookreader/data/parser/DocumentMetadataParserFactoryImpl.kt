package com.trackbool.bookreader.data.parser

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParser
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParserFactory
import javax.inject.Inject

class DocumentMetadataParserFactoryImpl @Inject constructor(
    private val parsers: Map<BookFileType, @JvmSuppressWildcards DocumentMetadataParser>
) : DocumentMetadataParserFactory {

    override fun getParser(fileType: BookFileType): DocumentMetadataParser? {
        return parsers[fileType]
    }
}
