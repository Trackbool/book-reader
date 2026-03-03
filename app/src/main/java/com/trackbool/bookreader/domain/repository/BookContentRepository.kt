package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.model.Chapter

interface BookContentRepository {
    suspend fun getContent(filePath: String): BookContent?
    suspend fun getChapter(filePath: String, chapterIndex: Int): Chapter?
    suspend fun getChapterCount(filePath: String): Int
    suspend fun invalidateCache()
}
