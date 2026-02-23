package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository

class AddBookUseCase(private val repository: BookRepository) {
    suspend operator fun invoke(title: String, author: String, totalPages: Int): Long {
        return repository.insertBook(
            Book(
                title = title,
                author = author,
                totalPages = totalPages
            )
        )
    }
}
