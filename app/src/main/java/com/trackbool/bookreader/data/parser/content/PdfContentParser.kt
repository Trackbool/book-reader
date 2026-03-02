package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.parser.content.BookContentParser
import java.io.File

class PdfContentParser : BookContentParser {

    override fun parse(file: File): BookContent? {
        return null
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.PDF
    }
}
