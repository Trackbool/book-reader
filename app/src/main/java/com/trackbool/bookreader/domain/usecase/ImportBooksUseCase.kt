package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.data.local.FileManager
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.source.BookSource

class ImportBooksUseCase(private val fileManager: FileManager) {
    suspend operator fun invoke(
        bookSources: List<BookSource>,
        titles: List<String>,
        authors: List<String>
    ): Result<List<Book>> {
        return try {
            val importResults = fileManager.importBooks(bookSources)
                .getOrElse { return Result.failure(it) }

            val books = importResults.mapIndexed { index, importResult ->
                Book(
                    title = titles.getOrElse(index) { importResult.fileName },
                    author = authors.getOrElse(index) { "" },
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
}
