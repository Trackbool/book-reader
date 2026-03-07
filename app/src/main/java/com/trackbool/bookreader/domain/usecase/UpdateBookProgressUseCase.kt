package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class UpdateBookProgressUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(book: Book, currentPage: Int, totalPages: Int): Result<Book> {
        val readingProgress = if (totalPages > 0) currentPage.toFloat() / totalPages else 0f
        val isCompleted = readingProgress >= 1f
        val updatedBook = book.copy(readingProgress = readingProgress, isCompleted = isCompleted)
        return repository.updateBook(updatedBook)
    }
}
