package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.data.parser.extractOpfPath
import com.trackbool.bookreader.data.parser.readEpubEntryAsString
import com.trackbool.bookreader.data.parser.resolvePath
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.Chapter
import com.trackbool.bookreader.domain.model.ChapterMetadata
import com.trackbool.bookreader.domain.model.BookContent
import com.trackbool.bookreader.domain.parser.content.BookContentParser
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

class EpubContentParser : BookContentParser {

    override fun parse(file: File): BookContent? {
        return try {
            ZipFile(file).use { zip -> parseEpub(zip, file.absolutePath) }
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
    private fun parseEpub(zip: ZipFile, filePath: String): BookContent {
        val opfPath = zip.extractOpfPath() ?: return BookContent(chapters = emptyList())
        val opfContent = zip.readEpubEntryAsString(opfPath) ?: return BookContent(chapters = emptyList())
        val opfDirectory = opfPath.substringBeforeLast("/", "")

        return BookContent(
            chapters = parseChapters(opfDirectory, opfContent, zip, filePath),
            language = extractLanguage(opfContent)
        )
    }

    /**
     * Builds the chapter list from the OPF spine, which defines reading order:
     *
     *   <manifest>
     *     <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
     *   </manifest>
     *   <spine>
     *     <itemref idref="chapter1"/>
     *     <itemref idref="chapter2"/>
     *   </spine>
     *
     * Each spine itemref points to a manifest item by id. The manifest item
     * provides the href to the actual HTML file inside the ZIP.
     */
    private fun parseChapters(
        opfDirectory: String,
        opfContent: String,
        zip: ZipFile,
        filePath: String
    ): List<Chapter> {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())
        val spine = doc.select("spine itemref")
        val manifest = doc.select("manifest item").associateBy { it.id() }

        return spine.mapIndexedNotNull { index, itemref ->
            val idref = itemref.attr("idref")
            val href = manifest[idref]?.attr("href") ?: return@mapIndexedNotNull null
            val chapterPath = resolvePath(opfDirectory, href)
            val rawContent = zip.readEpubEntryAsString(chapterPath) ?: ""
            val chapterDir = chapterPath.substringBeforeLast("/", "")

            Chapter(
                metadata = ChapterMetadata(
                    title = extractChapterTitle(rawContent),
                    chapterIndex = index
                ),
                content = rewriteImageSrcs(rawContent, chapterDir, filePath)
            )
        }
    }

    /**
     * Rewrites relative <img src> paths to a custom URI scheme:
     *
     *   ../images/cover.jpg  ->  epub:///abs/path/to/book.epub!OEBPS/images/cover.jpg
     *
     * Skips already-absolute URIs (data:, http:, epub://).
     */
    private fun rewriteImageSrcs(
        html: String,
        chapterDir: String,
        filePath: String
    ): String {
        val doc = Jsoup.parse(html)
        doc.select("img[src]").forEach { img ->
            val src = img.attr("src")
            if (!src.startsWith("epub://") && !src.startsWith("data:") && !src.startsWith("http")) {
                val rawZipPath = resolvePath(chapterDir, src)
                val normalizedZipPath = normalizePath(rawZipPath)
                img.attr("src", "epub://$filePath!$normalizedZipPath")
            }
        }
        return doc.toString()
    }

    /**
     * Resolves ".." and "." segments in a ZIP entry path.
     * e.g. "OEBPS/Text/../Images/cover.jpg" -> "OEBPS/Images/cover.jpg"
     */
    private fun normalizePath(path: String): String {
        val segments = ArrayDeque<String>()
        path.split("/").forEach { segment ->
            when (segment) {
                ".", "" -> Unit
                ".." -> segments.removeLastOrNull()
                else -> segments.addLast(segment)
            }
        }
        return segments.joinToString("/")
    }

    /**
     * Extracts the book language from the OPF metadata block.
     * Supports both Dublin Core namespaced and plain variants:
     *
     *   EPUB 2 / EPUB 3 (Dublin Core):
     *     <dc:language>en</dc:language>
     *
     *   Minimal OPF (no namespace):
     *     <language>es</language>
     */
    private fun extractLanguage(opfContent: String): String? {
        return Jsoup.parse(opfContent, "", Parser.xmlParser())
            .selectFirst("dc|language, language")?.text()
    }

    /**
     * Extracts the chapter title from the HTML content file.
     * Tries the following in order of priority:
     *
     *   1. <title>Chapter I</title>       — HTML head title
     *   2. <h1>Chapter I</h1>             — first top-level heading
     *   3. <h2>Chapter I</h2>             — second-level heading fallback
     *
     * Returns null if none are found or all are blank (e.g. auto-generated
     * nav documents or cover pages with no visible heading).
     */
    private fun extractChapterTitle(htmlContent: String): String? {
        val doc = Jsoup.parse(htmlContent)
        return doc.selectFirst("title")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h2")?.text()?.takeIf { it.isNotBlank() }
    }
}