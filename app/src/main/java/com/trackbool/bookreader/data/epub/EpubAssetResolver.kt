package com.trackbool.bookreader.data.epub

import com.trackbool.bookreader.domain.repository.AssetResolver
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubAssetResolver @Inject constructor() : AssetResolver {

    private val cache = ConcurrentHashMap<String, ZipFile>()

    override fun resolve(bookPath: String, path: String): InputStream? = try {
        val zip = cache.getOrPut(bookPath) { ZipFile(bookPath) }
        val entry = zip.getEntry(path) ?: return null
        zip.getInputStream(entry)
    } catch (e: Exception) {
        null
    }

    override fun release(bookPath: String) {
        cache.remove(bookPath)?.close()
    }

    override fun releaseAll() {
        cache.values.forEach { runCatching { it.close() } }
        cache.clear()
    }
}