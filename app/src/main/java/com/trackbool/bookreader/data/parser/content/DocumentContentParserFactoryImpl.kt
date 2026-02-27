package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import com.trackbool.bookreader.domain.parser.content.DocumentContentParserFactory
import javax.inject.Inject

class DocumentContentParserFactoryImpl @Inject constructor(
    private val parsers: Map<BookFileType, @JvmSuppressWildcards DocumentContentParser>
) : DocumentContentParserFactory {

    override fun getParser(fileType: BookFileType): DocumentContentParser? {
        return parsers[fileType]
    }
}
