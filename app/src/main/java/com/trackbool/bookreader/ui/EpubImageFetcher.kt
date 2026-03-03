package com.trackbool.bookreader.ui

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache that keeps ZipFile handles open across image requests for the same EPUB.
 *
 * Opening a ZipFile reads the central directory (~ms). With dozens of images per chapter
 * this cost adds up visibly. We keep one handle per book path and only close them when
 * the cache is explicitly cleared (e.g. when the reader is closed).
 *
 * Thread-safety: ConcurrentHashMap for the map + computeIfAbsent ensures a single ZipFile
 * per path even under concurrent Coil fetches.
 */
@Singleton
class EpubZipCache @Inject constructor() : Closeable {

    private val openZips = ConcurrentHashMap<String, ZipFile>()

    fun getOrOpen(bookPath: String): ZipFile =
        openZips.getOrPut(bookPath) { ZipFile(bookPath) }

    fun evict(bookPath: String) {
        openZips.remove(bookPath)?.close()
    }

    override fun close() {
        val entries = openZips.entries.toList()
        openZips.clear()
        entries.forEach { (_, zip) -> runCatching { zip.close() } }
    }
}

class EpubImageFetcher(
    private val uri: Uri,
    private val options: Options,
    private val zipCache: EpubZipCache
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // URI format: epub:///abs/path/to/book.epub!OEBPS/images/cover.jpg
        val raw = uri.toString().removePrefix("epub://")
        val separatorIndex = raw.lastIndexOf('!')
        check(separatorIndex != -1) { "Invalid epub URI: $uri" }

        val bookPath = raw.substring(0, separatorIndex)
        val entryPath = raw.substring(separatorIndex + 1)

        val bytes = withContext(Dispatchers.IO) {
            // Reuse the cached ZipFile — no open/close overhead per image
            val zip = zipCache.getOrOpen(bookPath)
            val entry = zip.getEntry(entryPath)
                ?: throw IOException("Entry not found: $entryPath in $bookPath")
            zip.getInputStream(entry).use { it.readBytes() }
        }

        return SourceResult(
            source = ImageSource(
                source = Buffer().apply { write(bytes) },
                context = options.context
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val zipCache: EpubZipCache) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "epub") return null
            return EpubImageFetcher(data, options, zipCache)
        }
    }
}