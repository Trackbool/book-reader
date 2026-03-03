package com.trackbool.bookreader.data.repository

import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Chapter
import com.trackbool.bookreader.domain.parser.content.BookContentParserFactory
import com.trackbool.bookreader.domain.repository.BookContentRepository
import com.trackbool.bookreader.domain.repository.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BookContentRepositoryImpl(
    private val fileManager: FileManager,
    private val parserFactory: BookContentParserFactory
) : BookContentRepository {

    private val cacheMutex = Mutex()
    private var cachedFilePath: String? = null
    private var cachedContent: BookContent? = null

    override suspend fun getContent(filePath: String): BookContent? =
        withContext(Dispatchers.IO) {
            ensureContentLoaded(filePath)
            cachedContent
        }

    override suspend fun getChapter(filePath: String, chapterIndex: Int): Chapter? =
        withContext(Dispatchers.IO) {
            ensureContentLoaded(filePath)
            cachedContent?.chapters?.getOrNull(chapterIndex)
        }

    override suspend fun getChapterCount(filePath: String): Int =
        withContext(Dispatchers.IO) {
            ensureContentLoaded(filePath)
            cachedContent?.totalChapters ?: 0
        }

    override suspend fun invalidateCache() {
        cacheMutex.withLock {
            cachedFilePath = null
            cachedContent = null
        }
    }

    private suspend fun ensureContentLoaded(filePath: String) {
        cacheMutex.withLock {
            if (cachedFilePath == filePath) return
            val file = fileManager.getFile(filePath)
            val fileType = BookFileType.fromExtension(file.extension)
            cachedContent = parserFactory.getParser(fileType)?.parse(file)
            cachedFilePath = filePath
        }
    }
}