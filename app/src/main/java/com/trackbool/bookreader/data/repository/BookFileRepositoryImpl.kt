package com.trackbool.bookreader.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.trackbool.bookreader.domain.model.Book
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Cover
import com.trackbool.bookreader.domain.repository.BookFileRepository
import com.trackbool.bookreader.domain.repository.FileManager
import com.trackbool.bookreader.domain.source.BookSource
import java.io.ByteArrayOutputStream
import java.util.UUID
import androidx.core.graphics.scale

class BookFileRepositoryImpl(
    private val fileManager: FileManager
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

            val booksDir = fileManager.ensureDirectoryExists(fileManager.getBooksDirectory())

            val destFile = fileManager.createFile(booksDir, newFileName)

            bookSource.openInputStream().use { inputStream ->
                fileManager.openOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val relativePath = "${fileManager.getBooksDirectory().name}/$newFileName"

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
                val file = fileManager.getFile(book.filePath)
                if (fileManager.exists(file)) {
                    fileManager.delete(file)
                }

                val cover = fileManager.getFile(book.coverPath)
                if (fileManager.exists(cover)) {
                    fileManager.delete(cover)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCoverImage(cover: Cover): String {
        val coversDir = fileManager.ensureDirectoryExists(fileManager.getCoversDirectory())

        val fileName = "${UUID.randomUUID()}.jpg"
        val coverFile = fileManager.createFile(coversDir, fileName)

        val scaled = scaleCoverImage(cover.bytes)
        coverFile.writeBytes(scaled)

        return "${fileManager.getCoversDirectory().name}/$fileName"
    }

    private fun scaleCoverImage(bytes: ByteArray, maxWidth: Int = 240, maxHeight: Int = 360): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return bytes

        val scale = minOf(
            maxWidth.toFloat() / original.width,
            maxHeight.toFloat() / original.height
        ).coerceAtMost(1f)

        if (scale == 1f) {
            original.recycle()
            return bytes
        }

        val scaled = original.scale(
            width = (original.width * scale).toInt(),
            height = (original.height * scale).toInt()
        )
        original.recycle()

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            scaled.recycle()
            out.toByteArray()
        }
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
}