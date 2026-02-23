package com.trackbool.bookreader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val description: String = "",
    val coverUrl: String = "",
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isCompleted: Boolean = false
)
