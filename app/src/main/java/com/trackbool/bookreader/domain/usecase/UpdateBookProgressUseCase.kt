package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class UpdateBookProgressUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(book: Book, currentPage: Int): Result<Book> {
        val isCompleted = currentPage >= book.totalPages
        val updatedBook = book.copy(currentPage = currentPage, isCompleted = isCompleted)
        return repository.updateBook(updatedBook)
    }
}
