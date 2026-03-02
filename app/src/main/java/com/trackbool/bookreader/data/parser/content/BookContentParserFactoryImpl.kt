package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.parser.content.BookContentParser
import com.trackbool.bookreader.domain.parser.content.BookContentParserFactory
import javax.inject.Inject

class BookContentParserFactoryImpl @Inject constructor(
    private val parsers: Map<BookFileType, @JvmSuppressWildcards BookContentParser>
) : BookContentParserFactory {

    override fun getParser(fileType: BookFileType): BookContentParser? {
        return parsers[fileType]
    }
}
