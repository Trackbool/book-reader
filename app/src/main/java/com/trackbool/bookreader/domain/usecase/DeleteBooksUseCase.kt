package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.data.local.FileManager
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class DeleteBooksUseCase(
    private val repository: BookRepository,
    private val fileManager: FileManager
) {
    suspend operator fun invoke(books: List<Book>): Result<List<Book>> {
        val result = repository.deleteBooks(books)

        if (result.isSuccess) {
            fileManager.deleteBookFiles(books)
        }

        return result
    }
}
