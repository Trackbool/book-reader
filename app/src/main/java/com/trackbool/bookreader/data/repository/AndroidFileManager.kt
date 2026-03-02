package com.trackbool.bookreader.data.repository

import android.content.Context
import com.trackbool.bookreader.domain.repository.FileManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class AndroidFileManager @Inject constructor(
    private val context: Context
) : FileManager {

    override fun getFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    override fun getBooksDirectory(): File {
        return getFile(BOOKS_DIR)
    }

    override fun getCoversDirectory(): File {
        return getFile(COVERS_DIR)
    }

    override fun createFile(parent: File, name: String): File {
        return File(parent, name)
    }

    override fun openInputStream(file: File): InputStream {
        return FileInputStream(file)
    }

    override fun openOutputStream(file: File): OutputStream {
        return FileOutputStream(file)
    }

    override fun delete(file: File): Boolean {
        return file.delete()
    }

    override fun exists(file: File): Boolean {
        return file.exists()
    }

    override fun ensureDirectoryExists(dir: File): File {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    companion object {
        private const val BOOKS_DIR = "books"
        private const val COVERS_DIR = "covers"
    }
}
