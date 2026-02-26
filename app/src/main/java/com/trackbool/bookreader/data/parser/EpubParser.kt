package com.trackbool.bookreader.data.parser

import android.content.Context
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.DocumentParser
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class EpubParser(
    private val context: Context
) : DocumentParser {

    override fun parse(file: File): DocumentMetadata? {
        return try {
            parseEpub(file)
        } catch (e: Exception) {
            null
        }
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.EPUB
    }

    private fun parseEpub(file: File): DocumentMetadata {
        val files = mutableMapOf<String, ByteArray>()

        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    files[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }

        val containerXml = files["META-INF/container.xml"]
            ?: return DocumentMetadata()

        val opfPath = extractOpfPath(containerXml.toString(Charsets.UTF_8))
            ?: return DocumentMetadata()

        val opfContent = files[opfPath]
            ?: return DocumentMetadata()

        val opfDirectory = opfPath.substringBeforeLast("/", "")

        val metadata = parseOpfMetadata(opfContent.toString(Charsets.UTF_8))

        val coverPath = extractCover(
            opfContent.toString(Charsets.UTF_8),
            files,
            opfDirectory
        )

        return metadata.copy(coverPath = coverPath)
    }

    private fun extractOpfPath(containerXml: String): String? {
        val doc = Jsoup.parse(containerXml, "", Parser.xmlParser())
        val rootFile = doc.selectFirst("rootfile")
        return rootFile?.attr("full-path")
    }

    private fun parseOpfMetadata(opfContent: String): DocumentMetadata {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())

        val title = doc.selectFirst("dc|title, title")?.text()
            ?: doc.selectFirst("metadata title")?.text()

        val author = doc.selectFirst("dc|creator, creator")?.text()
            ?: doc.selectFirst("metadata creator")?.text()

        val description = doc.selectFirst("dc|description, description")?.text()
            ?: doc.selectFirst("metadata description")?.text()

        return DocumentMetadata(
            title = title,
            author = author,
            description = description
        )
    }

    private fun extractCover(
        opfContent: String,
        files: Map<String, ByteArray>,
        opfDirectory: String
    ): String? {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())

        val metaCover = doc.selectFirst("meta[name=cover]")
        val coverId = metaCover?.attr("content")

        val manifestItem = if (coverId != null) {
            doc.selectFirst("manifest item[id=$coverId]")
        } else {
            doc.selectFirst("manifest item[properties=cover-image]")
        }

        val coverHref = manifestItem?.attr("href") ?: return null

        val coverPath = if (opfDirectory.isNotEmpty()) {
            "$opfDirectory/$coverHref"
        } else {
            coverHref
        }.replace("//", "/")

        val coverData = files[coverPath] ?: return null

        return saveCoverImage(coverData)
    }

    private fun saveCoverImage(coverData: ByteArray): String {
        val coversDir = File(context.filesDir, COVERS_DIR)
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val coverFile = File(coversDir, fileName)
        coverFile.writeBytes(coverData)

        return "$COVERS_DIR/$fileName"
    }

    companion object {
        private const val COVERS_DIR = "covers"
    }
}
