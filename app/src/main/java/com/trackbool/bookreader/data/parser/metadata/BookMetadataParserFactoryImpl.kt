package com.trackbool.bookreader.data.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParser
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParserFactory
import javax.inject.Inject

class BookMetadataParserFactoryImpl @Inject constructor(
    private val parsers: Map<BookFileType, @JvmSuppressWildcards BookMetadataParser>
) : BookMetadataParserFactory {

    override fun getParser(fileType: BookFileType): BookMetadataParser? {
        return parsers[fileType]
    }
}
