package com.trackbool.bookreader.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>): List<Long>

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBooks(books: List<BookEntity>)

    @Query("SELECT * FROM books WHERE isCompleted = 0 ORDER BY id DESC")
    fun getBooksInProgress(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isCompleted = 1 ORDER BY id DESC")
    fun getCompletedBooks(): Flow<List<BookEntity>>
}
