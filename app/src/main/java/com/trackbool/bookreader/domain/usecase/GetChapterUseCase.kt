package com.trackbool.bookreader.domain.usecase

import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.Chapter
import com.trackbool.bookreader.domain.repository.BookContentRepository

class GetChapterUseCase(private val repository: BookContentRepository) {
    suspend operator fun invoke(book: Book, chapterIndex: Int): Chapter? =
        repository.getChapter(book.filePath, chapterIndex)
}
