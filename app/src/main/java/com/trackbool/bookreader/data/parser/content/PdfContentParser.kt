package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentContent
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import java.io.File

class PdfContentParser : DocumentContentParser {

    override fun parse(file: File): DocumentContent? {
        return null
    }

    override fun loadChapterContent(file: File, chapterIndex: Int): String {
        TODO("Not yet implemented")
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.PDF
    }
}
