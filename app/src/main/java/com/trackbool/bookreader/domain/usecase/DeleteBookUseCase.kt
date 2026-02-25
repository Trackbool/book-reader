package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class DeleteBookUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(book: Book): Result<Book> {
        return repository.deleteBook(book)
    }
}
