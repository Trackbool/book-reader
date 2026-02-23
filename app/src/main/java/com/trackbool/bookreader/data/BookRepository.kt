package com.trackbool.bookreader.data

import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val booksInProgress: Flow<List<Book>> = bookDao.getBooksInProgress()
    val completedBooks: Flow<List<Book>> = bookDao.getCompletedBooks()

    suspend fun getBookById(id: Long): Book? = bookDao.getBookById(id)

    suspend fun insertBook(book: Book): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: Book) = bookDao.updateBook(book)

    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)
}
