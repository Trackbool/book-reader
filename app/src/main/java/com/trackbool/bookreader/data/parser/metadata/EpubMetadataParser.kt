package com.trackbool.bookreader.data.parser.metadata

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Cover
import com.trackbool.bookreader.domain.model.DocumentMetadata
import com.trackbool.bookreader.domain.parser.metadata.DocumentMetadataParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

class EpubMetadataParser : DocumentMetadataParser {

    override fun parse(file: File): DocumentMetadata? {
        return try {
            ZipFile(file).use { zip -> parseEpub(zip) }
        } catch (e: Exception) {
            null
        }
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.EPUB
    }

    private fun parseEpub(zip: ZipFile): DocumentMetadata {
        val containerXml = zip.readEntryAsString("META-INF/container.xml")
            ?: return DocumentMetadata()

        val opfPath = extractOpfPath(containerXml)
            ?: return DocumentMetadata()

        val opfContent = zip.readEntryAsString(opfPath)
            ?: return DocumentMetadata()

        val opfDirectory = opfPath.substringBeforeLast("/", "")

        val metadata = parseOpfMetadata(opfContent)
        val cover = extractCover(opfContent, zip, opfDirectory)

        return metadata.copy(cover = cover)
    }

    private fun extractOpfPath(containerXml: String): String? {
        val doc = Jsoup.parse(containerXml, "", Parser.xmlParser())
        return doc.selectFirst("rootfile")?.attr("full-path")
    }

    private fun parseOpfMetadata(opfContent: String): DocumentMetadata {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())
        val metadata = doc.selectFirst("metadata") ?: return DocumentMetadata()

        return DocumentMetadata(
            title       = extractTitle(metadata),
            author      = extractAuthor(metadata),
            description = extractDescription(metadata)
        )
    }

    /**
     * Supports three different title formats:
     *   EPUB 2/3 with namespace : <dc:title>Dune</dc:title>
     *   EPUB 2 without namespace : <title>Dune</title>
     *   EPUB 3 newest            : <meta property="dcterms:title">Dune</meta>
     */
    private fun extractTitle(metadata: Element): String? {
        return metadata.selectFirst("dc|title, title")?.text()
            ?: metadata.selectFirst("metadata title")?.text()
            ?: metadata.selectFirst("meta[property=dcterms:title]")?.text()
            ?: metadata.selectFirst("meta[property=title]")?.text()
    }

    private fun extractAuthor(metadata: Element): String? {
        // Try dc:creator with namespace
        var creators = metadata.select("dc|creator, creator")

        // Fallback EPUB 3
        if (creators.isEmpty()) {
            creators = metadata.select("meta[property=dcterms:creator], meta[property=creator]")
        }

        if (creators.isEmpty()) return null

        // If many authors, take only those with role="aut"
        val authorIds = metadata
            .select("meta[property=role]")
            .filter { it.text().equals("aut", ignoreCase = true) }
            .mapNotNull { it.attr("refines").removePrefix("#").takeIf { id -> id.isNotEmpty() } }
            .toSet()

        val authors = if (authorIds.isNotEmpty()) {
            creators.filter { it.id() in authorIds }.ifEmpty { creators }
        } else {
            creators
        }

        return authors.joinToString(", ") { it.text() }.takeIf { it.isNotEmpty() }
    }

    private fun extractDescription(metadata: Element): String? {
        return metadata.selectFirst("dc|description, description")?.text()
            ?: metadata.selectFirst("metadata description")?.text()
            ?: metadata.selectFirst("meta[property=dcterms:description]")?.text()
    }

    /**
     * Supports 3 ways in which the cover can be specified:
     *
     *   EPUB 2 (meta + manifest):
     *     <meta name="cover" content="cover-image"/>
     *     <item id="cover-image" href="cover.jpg" media-type="image/jpeg"/>
     *
     *   EPUB 3 (properties in manifest):
     *     <item properties="cover-image" href="images/cover.png" media-type="image/png"/>
     *
     *   EPUB 2 older (guide):
     *     <reference type="cover" href="cover.xhtml"/>
     *     → the .xhtml contains <img src="cover.jpg"/>
     */
    private fun extractCover(opfContent: String, zip: ZipFile, opfDirectory: String): Cover? {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())

        // EPUB 2 — <meta name="cover" content="id">
        val coverId = doc.selectFirst("meta[name=cover]")?.attr("content")
        if (coverId != null) {
            val item = doc.selectFirst("manifest item[id=$coverId]")
            val href = item?.attr("href")
            if (href != null) {
                val bytes = zip.readEntryAsBytes(resolvePath(opfDirectory, href))
                return buildCover(bytes, item.attr("media-type"))
            }
        }

        // EPUB 3 — <item properties="cover-image">
        val epub3Item = doc.selectFirst("manifest item[properties=cover-image]")
        val epub3Href = epub3Item?.attr("href")
        if (epub3Href != null) {
            val bytes = zip.readEntryAsBytes(resolvePath(opfDirectory, epub3Href))
            return buildCover(bytes, epub3Item.attr("media-type"))
        }

        // EPUB 2 older — <guide><reference type="cover" href="cover.xhtml">
        val guideHref = doc.selectFirst("guide reference[type=cover]")?.attr("href")
        if (guideHref != null) {
            val xhtmlPath = resolvePath(opfDirectory, guideHref)
            val xhtmlContent = zip.readEntryAsString(xhtmlPath)
            if (xhtmlContent != null) {
                val imgSrc = Jsoup.parse(xhtmlContent).selectFirst("img")?.attr("src")
                if (imgSrc != null) {
                    val xhtmlDir = xhtmlPath.substringBeforeLast("/", "")
                    val bytes = zip.readEntryAsBytes(resolvePath(xhtmlDir, imgSrc))
                    return buildCover(bytes)
                }
            }
        }

        return null
    }

    /**
     * Builds a [Cover] resolving media-type with priority:
     *   1. media-type declared in OPF
     *   2. Magic bytes from the file
     *   3. "application/octet-stream" as fallback
     */
    private fun buildCover(bytes: ByteArray?, mimeType: String? = null): Cover? {
        bytes ?: return null
        val resolved = mimeType?.takeIf { it.isNotEmpty() }
            ?: detectMimeType(bytes)
            ?: "application/octet-stream"
        return Cover(bytes, resolved)
    }

    private fun detectMimeType(bytes: ByteArray): String? = when {
        bytes.size < 4                                           -> null
        bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
        bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
        bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> "image/gif"
        bytes[0] == 0x52.toByte() && bytes[3] == 0x57.toByte() -> "image/webp"
        else                                                     -> null
    }

    private fun resolvePath(directory: String, href: String): String {
        return if (directory.isNotEmpty()) "$directory/$href" else href
    }

    private fun ZipFile.readEntryAsString(path: String): String? {
        val entry = getEntry(path) ?: return null
        val bytes = getInputStream(entry).use { it.readBytes() }
        val encoding = detectXmlEncoding(bytes) ?: Charsets.UTF_8
        return bytes.toString(encoding)
    }

    private fun ZipFile.readEntryAsBytes(path: String): ByteArray? {
        val entry = getEntry(path) ?: return null
        return getInputStream(entry).use { it.readBytes() }
    }

    /**
     * Detects the encoding declared in the XML header.
     *
     * Example:
     * <?xml version="1.0" encoding="ISO-8859-1"?> */
    private fun detectXmlEncoding(bytes: ByteArray): java.nio.charset.Charset? {
        val header = bytes.take(200).toByteArray().toString(Charsets.US_ASCII)
        val match = Regex("""encoding=["']([^"']+)["']""").find(header) ?: return null
        return runCatching { charset(match.groupValues[1]) }.getOrNull()
    }
}