package com.trackbool.bookreader.data.parser.metadata

import com.trackbool.bookreader.data.parser.extractOpfPath
import com.trackbool.bookreader.data.parser.readEpubEntryAsBytes
import com.trackbool.bookreader.data.parser.readEpubEntryAsString
import com.trackbool.bookreader.data.parser.resolvePath
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

    override fun supports(fileType: BookFileType) = fileType == BookFileType.EPUB

    /**
     * Locates the OPF file via the META-INF/container.xml entry:
     *
     *   <container>
     *     <rootfiles>
     *       <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
     *     </rootfiles>
     *   </container>
     *
     * The OPF path is used to resolve all relative hrefs in the manifest.
     */
    private fun parseEpub(zip: ZipFile): DocumentMetadata {
        val opfPath = zip.extractOpfPath() ?: return DocumentMetadata()
        val opfContent = zip.readEpubEntryAsString(opfPath) ?: return DocumentMetadata()
        val opfDirectory = opfPath.substringBeforeLast("/", "")

        val metadata = parseOpfMetadata(opfContent)
        val cover = extractCover(opfContent, zip, opfDirectory)

        return metadata.copy(cover = cover)
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

    /**
     * Supports two author formats and optional role refinement:
     *
     *   EPUB 2/3 (Dublin Core):
     *     <dc:creator>Frank Herbert</dc:creator>
     *
     *   EPUB 3 with role refinement (filters to role="aut" when present):
     *     <dc:creator id="author1">Frank Herbert</dc:creator>
     *     <meta refines="#author1" property="role">aut</meta>
     *
     * Multiple authors are joined with ", ".
     */
    private fun extractAuthor(metadata: Element): String? {
        var creators = metadata.select("dc|creator, creator")

        if (creators.isEmpty()) {
            creators = metadata.select("meta[property=dcterms:creator], meta[property=creator]")
        }

        if (creators.isEmpty()) return null

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

    /**
     * Supports three description formats:
     *   EPUB 2/3 (Dublin Core):  <dc:description>...</dc:description>
     *   EPUB 2 without namespace: <description>...</description>
     *   EPUB 3 newest:            <meta property="dcterms:description">...</meta>
     */
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

        val coverId = doc.selectFirst("meta[name=cover]")?.attr("content")
        if (coverId != null) {
            val item = doc.selectFirst("manifest item[id=$coverId]")
            val href = item?.attr("href")
            if (href != null) {
                val bytes = zip.readEpubEntryAsBytes(resolvePath(opfDirectory, href))
                return buildCover(bytes, item.attr("media-type"))
            }
        }

        val epub3Item = doc.selectFirst("manifest item[properties=cover-image]")
        val epub3Href = epub3Item?.attr("href")
        if (epub3Href != null) {
            val bytes = zip.readEpubEntryAsBytes(resolvePath(opfDirectory, epub3Href))
            return buildCover(bytes, epub3Item.attr("media-type"))
        }

        val guideHref = doc.selectFirst("guide reference[type=cover]")?.attr("href")
        if (guideHref != null) {
            val xhtmlPath = resolvePath(opfDirectory, guideHref)
            val xhtmlContent = zip.readEpubEntryAsString(xhtmlPath)
            if (xhtmlContent != null) {
                val imgSrc = Jsoup.parse(xhtmlContent).selectFirst("img")?.attr("src")
                if (imgSrc != null) {
                    val xhtmlDir = xhtmlPath.substringBeforeLast("/", "")
                    val bytes = zip.readEpubEntryAsBytes(resolvePath(xhtmlDir, imgSrc))
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
}