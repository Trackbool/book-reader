package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.repository.BookRepository

class DeleteBooksUseCase(
    private val repository: BookRepository,
    private val bookFileRepository: BookFileRepository
) {
    suspend operator fun invoke(books: List<Book>): Result<List<Book>> {
        val result = repository.deleteBooks(books)

        if (result.isSuccess) {
            bookFileRepository.deleteBookFiles(books)
        }

        return result
    }
}
