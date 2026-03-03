package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow

class GetBookUseCase(private val repository: BookRepository) {
    operator fun invoke(id: Long): Flow<Book> = repository.getBookById(id)
}