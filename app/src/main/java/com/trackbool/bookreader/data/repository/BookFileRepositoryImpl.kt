package com.trackbool.bookreader.data.repository

import android.content.Context
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Cover
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParserFactory
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.source.BookSource
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BookFileRepositoryImpl(
    private val context: Context,
    private val parserFactory: DocumentMetadataParserFactory
) : BookFileRepository {

    override suspend fun importBooks(bookSources: List<BookSource>): Result<List<BookFileRepository.ImportResult>> {
        return try {
            val results = bookSources.mapNotNull { bookSource ->
                importBook(bookSource).getOrNull()
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun importBook(bookSource: BookSource): Result<BookFileRepository.ImportResult> {
        return try {
            val fileName = bookSource.getFileName() ?: "unknown_${UUID.randomUUID()}"
            val extension = getFileExtension(fileName)
            val fileType = BookFileType.fromExtension(extension)

            val uuid = UUID.randomUUID().toString()
            val newFileName = "$uuid.$extension"

            val booksDir = File(context.filesDir, BOOKS_DIR)
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }

            val destFile = File(booksDir, newFileName)

            bookSource.openInputStream().use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val relativePath = "$BOOKS_DIR/$newFileName"

            Result.success(
                BookFileRepository.ImportResult(
                    filePath = relativePath,
                    fileType = fileType,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBookFiles(books: List<Book>): Result<Unit> {
        return try {
            books.forEach { book ->
                val file = File(context.filesDir, book.filePath)
                if (file.exists()) {
                    file.delete()
                }

                val cover = File(context.filesDir, book.coverPath)
                if (cover.exists()) {
                    cover.delete()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCoverImage(cover: Cover): String {
        val coversDir = File(context.filesDir, COVERS_DIR)
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }

        val fileName = "${UUID.randomUUID()}.${cover.extension}"
        val coverFile = File(coversDir, fileName)
        coverFile.writeBytes(cover.bytes)

        return "$COVERS_DIR/$fileName"
    }

    override suspend fun extractMetadata(filePath: String, fileType: BookFileType): DocumentMetadata? {
        val file = File(context.filesDir, filePath)
        return parserFactory.getParser(fileType)?.parse(file)
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    companion object {
        private const val BOOKS_DIR = "books"
        private const val COVERS_DIR = "covers"
    }
}
