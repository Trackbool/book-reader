package com.trackbool.bookreader.data.repository

import android.content.Context
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookMetadata
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParserFactory
import com.trackbool.bookreader.domain.repository.BookMetadataRepository
import java.io.File

class BookMetadataRepositoryImpl(
    private val context: Context,
    private val parserFactory: BookMetadataParserFactory
): BookMetadataRepository {

    override suspend fun extractMetadata(filePath: String, fileType: BookFileType): BookMetadata? {
        val file = File(context.filesDir, filePath)
        return parserFactory.getParser(fileType)?.parse(file)
    }
}