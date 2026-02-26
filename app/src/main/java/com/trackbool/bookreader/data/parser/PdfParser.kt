package com.trackbool.bookreader.data.parser

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.DocumentParser
import java.io.File
import javax.inject.Inject

class PdfParser @Inject constructor() : DocumentParser {

    override fun parse(file: File): DocumentMetadata? {
        return null
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.PDF
    }
}
