package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParserFactory
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.source.BookSource

class ImportBooksUseCase(
    private val bookFileRepository: BookFileRepository,
    private val parserFactory: DocumentMetadataParserFactory
) {
    suspend operator fun invoke(
        bookSources: List<BookSource>
    ): Result<List<Book>> {
        return try {
            val importResults = bookFileRepository.importBooks(bookSources)
                .getOrElse { return Result.failure(it) }

            val books = importResults.mapIndexed { _, importResult ->
                val metadata = extractMetadata(importResult.filePath, importResult.fileType)

                val coverPath = metadata?.coverBytes?.let { coverBytes ->
                    bookFileRepository.saveCoverImage(coverBytes)
                }

                Book(
                    title = metadata?.title ?: importResult.fileName.substringBeforeLast("."),
                    author = metadata?.author ?: "",
                    description = metadata?.description ?: "",
                    coverPath = coverPath ?: "",
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
        val file = bookFileRepository.getBookFile(filePath)
        return parserFactory.getParser(fileType)?.parse(file)
    }
}
