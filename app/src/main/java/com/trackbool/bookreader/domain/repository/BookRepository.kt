package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getBooksInProgress(): Flow<List<Book>>
    fun getCompletedBooks(): Flow<List<Book>>
    suspend fun getBookById(id: Long): Result<Book>
    suspend fun insertBook(book: Book): Result<Book>
    suspend fun updateBook(book: Book): Result<Book>
    suspend fun deleteBook(book: Book): Result<Book>
}
