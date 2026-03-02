package com.trackbool.bookreader.domain.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookMetadata
import java.io.File

interface DocumentMetadataParser {
    fun parse(file: File): BookMetadata?
    fun supports(fileType: BookFileType): Boolean
}
