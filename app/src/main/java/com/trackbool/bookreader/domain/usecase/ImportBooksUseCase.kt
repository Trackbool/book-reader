package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.data.local.FileManager
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.DocumentParserFactory
import com.trackbool.bookreader.domain.source.BookSource

class ImportBooksUseCase(
    private val fileManager: FileManager,
    private val parserFactory: DocumentParserFactory
) {
    suspend operator fun invoke(
        bookSources: List<BookSource>
    ): Result<List<Book>> {
        return try {
            val importResults = fileManager.importBooks(bookSources)
                .getOrElse { return Result.failure(it) }

            val books = importResults.mapIndexed { _, importResult ->
                val metadata = extractMetadata(importResult.filePath, importResult.fileType)

                Book(
                    title = metadata?.title ?: importResult.fileName.substringBeforeLast("."),
                    author = metadata?.author ?: "",
                    description = metadata?.description ?: "",
                    coverPath = metadata?.coverPath ?: "",
                    filePath = importResult.filePath,
                    fileType = importResult.fileType,
                    fileName = importResult.fileName
                )
            }

            Result.success(books)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractMetadata(filePath: String, fileType: BookFileType): DocumentMetadata? {
        val file = fileManager.getBookFile(filePath)
        return parserFactory.getParser(fileType)?.parse(file)
    }
}
