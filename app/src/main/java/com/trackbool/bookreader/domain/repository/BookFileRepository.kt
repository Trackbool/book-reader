package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Cover
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.source.BookSource
import java.io.File

interface BookFileRepository {
    suspend fun importBooks(bookSources: List<BookSource>): Result<List<ImportResult>>
    suspend fun deleteBookFiles(books: List<Book>): Result<Unit>
    suspend fun saveCoverImage(cover: Cover): String
    suspend fun extractMetadata(filePath: String, fileType: BookFileType): DocumentMetadata?

    data class ImportResult(
        val filePath: String,
        val fileType: BookFileType,
        val fileName: String
    )
}
