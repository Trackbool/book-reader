package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.repository.BookContentRepository

class GetChapterCountUseCase(private val repository: BookContentRepository) {
    suspend operator fun invoke(book: Book): Int =
        repository.getChapterCount(book.filePath)
}
