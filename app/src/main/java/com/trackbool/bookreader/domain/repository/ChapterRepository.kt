package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.BookContent
import java.io.File

interface ChapterRepository {
    suspend fun getDocumentContent(file: File): BookContent?
    fun invalidateCache()
}
