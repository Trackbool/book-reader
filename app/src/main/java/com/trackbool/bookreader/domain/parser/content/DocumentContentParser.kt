package com.trackbool.bookreader.domain.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentContent
import java.io.File

interface DocumentContentParser {
    fun parse(file: File): DocumentContent?
    fun loadChapterContent(file: File, chapterIndex: Int): String
    fun supports(fileType: BookFileType): Boolean
}