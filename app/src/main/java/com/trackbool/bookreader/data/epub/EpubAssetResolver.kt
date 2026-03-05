package com.trackbool.bookreader.data.epub

import com.trackbool.bookreader.domain.repository.AssetResolver
import com.trackbool.bookreader.domain.repository.FileManager
import java.io.InputStream
import java.util.zip.ZipFile

class EpubAssetResolver(
    private val fileManager: FileManager,
    private val filePath: String,
) : AssetResolver {

    private var zipFile: ZipFile? = null

    override fun resolve(path: String): InputStream? = try {
        val zip = getZipFile() ?: return null
        val entry = zip.getEntry(path) ?: return null
        zip.getInputStream(entry)
    } catch (_: Exception) {
        null
    }

    override fun release() {
        zipFile?.close()
        zipFile = null
    }

    private fun getZipFile(): ZipFile? {
        if (zipFile == null) {
            val file = fileManager.getFile(filePath)
            zipFile = ZipFile(file)
        }
        return zipFile
    }
}
