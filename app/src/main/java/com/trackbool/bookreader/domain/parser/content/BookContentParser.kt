package com.trackbool.bookreader.domain.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookContent
import java.io.File

interface BookContentParser {
    fun parse(file: File): BookContent?
    fun supports(fileType: BookFileType): Boolean
}