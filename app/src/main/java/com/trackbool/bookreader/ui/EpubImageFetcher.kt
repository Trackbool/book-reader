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
import java.io.IOException
import java.util.zip.ZipFile

class EpubImageFetcher(
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // epub:///storage/emulated/0/books/mybook.epub!OEBPS/images/cover.jpg
        val raw = uri.toString().removePrefix("epub://")
        val separatorIndex = raw.lastIndexOf('!')
        check(separatorIndex != -1) { "Invalid epub URI: $uri" }

        val bookPath = raw.substring(0, separatorIndex)
        val zipEntry = raw.substring(separatorIndex + 1)

        val bytes = withContext(Dispatchers.IO) {
            ZipFile(bookPath).use { zip ->
                zip.getEntry(zipEntry)?.let { entry ->
                    zip.getInputStream(entry).use { it.readBytes() }
                }
            }
        } ?: throw IOException("Entry not found: $zipEntry in $bookPath")

        return SourceResult(
            source = ImageSource(
                source = Buffer().apply { write(bytes) },
                context = options.context
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "epub") return null
            return EpubImageFetcher(data, options)
        }
    }
}