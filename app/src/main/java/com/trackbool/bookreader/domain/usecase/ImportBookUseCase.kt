package com.trackbool.bookreader.domain.usecase

import android.net.Uri
import com.trackbool.bookreader.data.local.FileManager
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class ImportBookUseCase(
    private val repository: BookRepository,
    private val fileManager: FileManager
) {
    suspend operator fun invoke(
        uri: Uri,
        title: String,
        author: String
    ): Result<Book> {
        return try {
            val importResult = fileManager.importBook(uri)
                .getOrElse { return Result.failure(it) }

            val book = Book(
                title = title,
                author = author,
                filePath = importResult.filePath,
                fileType = importResult.fileType,
                fileName = importResult.fileName
            )

            val id = repository.insertBook(book)
            Result.success(book.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
