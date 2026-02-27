package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.Chapter
import java.io.File

interface ChapterRepository {
    suspend fun getChapter(file: File, chapterIndex: Int): Chapter?
    suspend fun getAdjacentChapters(file: File, currentIndex: Int): Pair<Chapter?, Chapter?>
    fun invalidateCache()
}
