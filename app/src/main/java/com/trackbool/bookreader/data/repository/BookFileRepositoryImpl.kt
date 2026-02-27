package com.trackbool.bookreader.data.repository

import android.content.Context
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.source.BookSource
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class BookFileRepositoryImpl(
    private val context: Context
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
            val fileType = getFileType(extension)

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

    override fun getBookFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    override suspend fun saveCoverImage(coverBytes: ByteArray): String {
        val coversDir = File(context.filesDir, COVERS_DIR)
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val coverFile = File(coversDir, fileName)
        coverFile.writeBytes(coverBytes)

        return "$COVERS_DIR/$fileName"
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    private fun getFileType(extension: String): BookFileType {
        return when (extension) {
            "pdf" -> BookFileType.PDF
            "epub" -> BookFileType.EPUB
            else -> BookFileType.NONE
        }
    }

    companion object {
        private const val BOOKS_DIR = "books"
        private const val COVERS_DIR = "covers"
    }
}
