package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun getBooksInProgress(): Flow<List<Book>>
    fun getCompletedBooks(): Flow<List<Book>>
    fun getBookById(id: Long): Flow<Book>
    suspend fun insertBooks(books: List<Book>): Result<List<Book>>
    suspend fun updateBook(book: Book): Result<Book>
    suspend fun deleteBooks(books: List<Book>): Result<List<Book>>
}
