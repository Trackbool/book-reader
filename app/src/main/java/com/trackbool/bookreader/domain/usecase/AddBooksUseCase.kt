package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class AddBooksUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(books: List<Book>): Result<List<Book>> {
        return repository.insertBooks(books)
    }
}
