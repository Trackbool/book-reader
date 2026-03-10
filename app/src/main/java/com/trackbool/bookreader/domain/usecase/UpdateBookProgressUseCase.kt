package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class UpdateBookProgressUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(
        book: Book,
        readingProgress: Float,
        documentPositionData: String = ""
    ): Result<Book> {
        val isCompleted = readingProgress >= 1f
        val updatedBook = book.copy(
            readingProgress = readingProgress,
            isCompleted = isCompleted,
            documentPositionData = documentPositionData
        )
        return repository.updateBook(updatedBook)
    }
}
