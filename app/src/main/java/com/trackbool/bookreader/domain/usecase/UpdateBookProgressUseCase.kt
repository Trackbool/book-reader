package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class UpdateBookProgressUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(book: Book, currentPage: Int) {
        val isCompleted = currentPage >= book.totalPages
        repository.updateBook(book.copy(currentPage = currentPage, isCompleted = isCompleted))
    }
}
