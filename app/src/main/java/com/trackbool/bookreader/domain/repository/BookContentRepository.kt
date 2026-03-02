package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.BookContent

interface BookContentRepository {
    suspend fun getContent(filePath: String): BookContent?
    fun invalidateCache()
}
