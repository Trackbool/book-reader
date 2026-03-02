package com.trackbool.bookreader.data.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParser
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParserFactory
import javax.inject.Inject

class BookMetadataParserFactoryImpl @Inject constructor(
    private val parsers: Map<BookFileType, @JvmSuppressWildcards DocumentMetadataParser>
) : BookMetadataParserFactory {

    override fun getParser(fileType: BookFileType): DocumentMetadataParser? {
        return parsers[fileType]
    }
}
