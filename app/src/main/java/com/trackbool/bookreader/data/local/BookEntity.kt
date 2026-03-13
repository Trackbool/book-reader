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
    val readingProgress: Float = 0f,
    val isCompleted: Boolean = false,
    val filePath: String = "",
    val fileType: String = "NONE",
    val fileName: String = "",
    val currentChapterId: String? = null,
    val documentPositionData: String = ""
)
