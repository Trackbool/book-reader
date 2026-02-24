package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class AddBookUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(book: Book): Result<Book> {
        return try {
            val id = repository.insertBook(book)
            Result.success(book.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
