package com.trackbool.bookreader.data.local

import android.content.Context
import android.net.Uri
import com.trackbool.bookreader.domain.model.BookFileType
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class FileManager(private val context: Context) {

    data class ImportResult(
        val filePath: String,
        val fileType: BookFileType,
        val fileName: String
    )

    fun importBook(uri: Uri): Result<ImportResult> {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(uri) ?: "unknown_${UUID.randomUUID()}"
            val extension = getFileExtension(fileName)
            val fileType = getFileType(extension)

            val uuid = UUID.randomUUID().toString()
            val newFileName = "$uuid.$extension"

            val booksDir = File(context.filesDir, BOOKS_DIR)
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }

            val destFile = File(booksDir, newFileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return Result.failure(Exception("No se pudo abrir el archivo"))

            val relativePath = "$BOOKS_DIR/$newFileName"

            Result.success(
                ImportResult(
                    filePath = relativePath,
                    fileType = fileType,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteBookFile(relativePath: String): Result<Unit> {
        return try {
            val file = File(context.filesDir, relativePath)
            if (file.exists()) {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getBookFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
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
        private const val BOOKS_DIR = "libros"
    }
}
