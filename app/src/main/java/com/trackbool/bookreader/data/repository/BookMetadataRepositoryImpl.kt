package com.trackbool.bookreader.data.repository

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.BookMetadata
import com.trackbool.bookreader.domain.parser.metadata.BookMetadataParserFactory
import com.trackbool.bookreader.domain.repository.BookMetadataRepository
import com.trackbool.bookreader.domain.repository.FileManager
import java.io.File

class BookMetadataRepositoryImpl(
    private val fileManager: FileManager,
    private val parserFactory: BookMetadataParserFactory
): BookMetadataRepository {

    override suspend fun extractMetadata(filePath: String, fileType: BookFileType): BookMetadata? {
        val file = fileManager.getFile(filePath)
        return parserFactory.getParser(fileType)?.parse(file)
    }
}