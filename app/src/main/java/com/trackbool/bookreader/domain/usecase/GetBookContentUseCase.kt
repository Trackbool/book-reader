package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.repository.BookContentRepository

class GetBookContentUseCase(private val repository: BookContentRepository) {
    suspend operator fun invoke(book: Book): BookContent? = repository.getContent(book.filePath)
}