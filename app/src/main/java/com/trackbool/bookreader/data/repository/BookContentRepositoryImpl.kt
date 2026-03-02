package com.trackbool.bookreader.data.repository

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookContent
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

    override suspend fun getContent(filePath: String): BookContent? {
        return withContext(Dispatchers.IO) {
            ensureContentLoaded(filePath)
            cachedContent
        }
    }

    override fun invalidateCache() {
        cachedFilePath = null
        cachedContent = null
    }

    private suspend fun ensureContentLoaded(filePath: String) {
        cacheMutex.withLock {
            if (cachedFilePath == null || cachedFilePath != filePath) {
                cachedFilePath = filePath
                val file = fileManager.getFile(filePath)
                val fileType = BookFileType.fromExtension(file.extension)
                val parser = parserFactory.getParser(fileType)
                cachedContent = parser?.parse(file)
            }
        }
    }
}