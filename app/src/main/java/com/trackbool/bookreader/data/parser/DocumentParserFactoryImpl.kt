package com.trackbool.bookreader.data.parser

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.DocumentParser
import com.trackbool.bookreader.domain.parser.DocumentParserFactory
import javax.inject.Inject

class DocumentParserFactoryImpl @Inject constructor(
    private val parsers: Map<BookFileType, @JvmSuppressWildcards DocumentParser>
) : DocumentParserFactory {

    override fun getParser(fileType: BookFileType): DocumentParser? {
        return parsers[fileType]
    }
}
