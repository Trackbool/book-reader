package com.trackbool.bookreader.domain.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import java.io.File

interface DocumentContentParserFactory {
    fun getParser(fileType: BookFileType): DocumentContentParser?
}
