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
                content = rewriteResourceUrls(rawContent, chapterDir, filePath)
            )
        }
    }

    /**
     * Rewrites all relative resource references to the custom epub:// scheme
     * so that WebView can intercept and serve them from the ZIP via shouldInterceptRequest.
     *
     * Covers:
     *   <img src>                  — raster images
     *   <image xlink:href>         — SVG images (cover pages)
     *   <link href>                — stylesheets
     *   <script src>               — javascript
     *   <audio src>, <video src>   — media
     *   <source src>               — media sources
     *
     * Skips already-absolute URIs (epub://, data:, http:, https:, //).
     */
    private fun rewriteResourceUrls(
        html: String,
        chapterDir: String,
        filePath: String
    ): String {
        val doc = Jsoup.parse(html)

        // selector -> attribute pairs to rewrite
        val targets = listOf(
            "img[src]"          to "src",
            "image[xlink:href]" to "xlink:href",
            "link[href]"        to "href",
            "script[src]"       to "src",
            "audio[src]"        to "src",
            "video[src]"        to "src",
            "source[src]"       to "src",
        )

        targets.forEach { (selector, attr) ->
            doc.select(selector).forEach { element ->
                val original = element.attr(attr)
                if (!original.isAbsoluteUri()) {
                    val resolved = normalizePath(resolvePath(chapterDir, original))
                    element.attr(attr, "epub://$filePath!$resolved")
                }
            }
        }

        return doc.toString()
    }

    private fun String.isAbsoluteUri(): Boolean =
        startsWith("epub://")
                || startsWith("data:")
                || startsWith("http:")
                || startsWith("https:")
                || startsWith("//")
                || isEmpty()

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