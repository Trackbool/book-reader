package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import java.io.File

class PdfContentParser : DocumentContentParser {

    override fun parse(file: File): BookContent? {
        return null
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.PDF
    }
}
