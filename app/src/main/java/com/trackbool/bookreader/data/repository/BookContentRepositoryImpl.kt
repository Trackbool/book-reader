package com.trackbool.bookreader.data.repository

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.parser.content.BookContentParserFactory
import com.trackbool.bookreader.domain.repository.BookContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class BookContentRepositoryImpl @Inject constructor(
    private val parserFactory: BookContentParserFactory
) : BookContentRepository {

    private val cacheMutex = Mutex()
    private var cachedFile: File? = null
    private var cachedContent: BookContent? = null

    override suspend fun getDocumentContent(file: File): BookContent? {
        return withContext(Dispatchers.IO) {
            ensureContentLoaded(file)
            cachedContent
        }
    }

    override fun invalidateCache() {
        cachedFile = null
        cachedContent = null
    }

    private suspend fun ensureContentLoaded(file: File) {
        cacheMutex.withLock {
            if (cachedFile == null || cachedFile != file) {
                cachedFile = file
                val fileType = BookFileType.fromExtension(file.extension)
                val parser = parserFactory.getParser(fileType)
                cachedContent = parser?.parse(file)
            }
        }
    }
}