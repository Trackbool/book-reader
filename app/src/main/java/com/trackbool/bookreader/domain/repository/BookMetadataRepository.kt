package com.trackbool.bookreader.domain.repository

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookMetadata

interface BookMetadataRepository {
    suspend fun extractMetadata(filePath: String, fileType: BookFileType): BookMetadata?
}