package com.trackbool.bookreader.data.repository

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Chapter
import com.trackbool.bookreader.domain.model.ChapterMetadata
import com.trackbool.bookreader.domain.model.DocumentContent
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import com.trackbool.bookreader.domain.parser.content.DocumentContentParserFactory
import com.trackbool.bookreader.domain.repository.ChapterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class ChapterRepositoryImpl @Inject constructor(
    private val parserFactory: DocumentContentParserFactory
) : ChapterRepository {

    private val cacheMutex = Mutex()
    private var cachedFile: File? = null
    private var cachedContent: DocumentContent? = null
    private var currentIndex: Int = -1
    private val window = LinkedHashMap<Int, Chapter>(3, 0.75f, true)

    override suspend fun getChapter(file: File, chapterIndex: Int): Chapter? {
        return withContext(Dispatchers.IO) {
            ensureContentLoaded(file)

            val metadata = cachedContent?.chapters?.getOrNull(chapterIndex) ?: return@withContext null

            window[chapterIndex]?.let { return@withContext it }

            val chapter = loadChapterContent(file, chapterIndex, metadata)
            chapter?.let { it ->
                window[chapterIndex] = it

                while (window.size > 3) {
                    val oldest = window.entries.first().key
                    if (oldest != chapterIndex - 1 && oldest != chapterIndex && oldest != chapterIndex + 1) {
                        window.remove(oldest)
                    } else {
                        break
                    }
                }

                window.keys.filter { it < chapterIndex - 1 || it > chapterIndex + 1 }.forEach {
                    window.remove(it)
                }
            }

            chapter
        }
    }

    override suspend fun getAdjacentChapters(file: File, currentIndex: Int): Pair<Chapter?, Chapter?> {
        return withContext(Dispatchers.IO) {
            ensureContentLoaded(file)

            val prevChapter = if (currentIndex > 0) {
                getChapter(file, currentIndex - 1)
            } else null

            val nextChapter = if (currentIndex < (cachedContent?.totalChapters ?: 0) - 1) {
                getChapter(file, currentIndex + 1)
            } else null

            Pair(prevChapter, nextChapter)
        }
    }

    override fun invalidateCache() {
        cachedFile = null
        cachedContent = null
        currentIndex = -1
        window.clear()
    }

    private suspend fun ensureContentLoaded(file: File) {
        cacheMutex.withLock {
            if (cachedFile == null || cachedFile != file) {
                cachedFile = file
                window.clear()

                val fileType = BookFileType.fromExtension(file.extension)

                val parser = parserFactory.getParser(fileType)
                cachedContent = parser?.parse(file)
            }
        }
    }

    private fun loadChapterContent(file: File, chapterIndex: Int, metadata: ChapterMetadata): Chapter? {
        val fileType = BookFileType.fromExtension(file.extension)

        val parser = parserFactory.getParser(fileType) ?: return null
        val content = parser.loadChapterContent(file, chapterIndex)

        return Chapter(
            metadata = metadata,
            content = content
        )
    }
}
