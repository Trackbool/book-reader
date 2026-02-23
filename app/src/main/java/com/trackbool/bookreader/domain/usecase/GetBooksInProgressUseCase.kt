package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow

class GetBooksInProgressUseCase(private val repository: BookRepository) {
    operator fun invoke(): Flow<List<Book>> = repository.getBooksInProgress()
}
