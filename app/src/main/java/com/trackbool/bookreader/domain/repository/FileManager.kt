package com.trackbool.bookreader.domain.repository

import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface FileManager {
    fun getFile(relativePath: String): File
    fun getBooksDirectory(): File
    fun getCoversDirectory(): File
    fun createFile(parent: File, name: String): File
    fun openInputStream(file: File): InputStream
    fun openOutputStream(file: File): OutputStream
    fun delete(file: File): Boolean
    fun exists(file: File): Boolean
    fun ensureDirectoryExists(dir: File): File
}
