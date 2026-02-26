package com.trackbool.bookreader.domain.parser

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentMetadata
import java.io.File

interface DocumentParser {
    fun parse(file: File): DocumentMetadata?
    fun supports(fileType: BookFileType): Boolean
}
