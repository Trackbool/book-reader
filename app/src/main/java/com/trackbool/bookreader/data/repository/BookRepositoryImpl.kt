package com.trackbool.bookreader.data.repository

import com.trackbool.bookreader.data.local.BookDao
import com.trackbool.bookreader.data.local.BookEntity
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepositoryImpl(private val bookDao: BookDao) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> =
        bookDao.getAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getBooksInProgress(): Flow<List<Book>> =
        bookDao.getBooksInProgress().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getCompletedBooks(): Flow<List<Book>> =
        bookDao.getCompletedBooks().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getBookById(id: Long): Book? =
        bookDao.getBookById(id)?.toDomain()

    override suspend fun insertBook(book: Book): Long =
        bookDao.insertBook(book.toEntity())

    override suspend fun updateBook(book: Book) =
        bookDao.updateBook(book.toEntity())

    override suspend fun deleteBook(book: Book) =
        bookDao.deleteBook(book.toEntity())

    private fun Book.toEntity() = BookEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        coverUrl = coverUrl,
        currentPage = currentPage,
        totalPages = totalPages,
        isCompleted = isCompleted,
        filePath = filePath,
        fileType = fileType.name,
        fileName = fileName
    )

    private fun BookEntity.toDomain() = Book(
        id = id,
        title = title,
        author = author,
        description = description,
        coverUrl = coverUrl,
        currentPage = currentPage,
        totalPages = totalPages,
        isCompleted = isCompleted,
        filePath = filePath,
        fileType = try { BookFileType.valueOf(fileType) } catch (e: Exception) { BookFileType.NONE },
        fileName = fileName
    )
}
