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
import org.jsoup.nodes.Document
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
     *     <item id="c001" href="xhtml/chapter1.xhtml" media-type="application/xhtml+xml"/>
     *   </manifest>
     *   <spine>
     *     <itemref idref="c001"/>
     *   </spine>
     *
     * The manifest item id (idref) is used as the canonical DOM id for each chapter <section>.
     * It is unique within the EPUB by spec, and stable across parse runs.
     *
     * All internal ids within each chapter are prefixed with their chapter's manifestId
     * (e.g. "intro" → "c001__intro") to guarantee uniqueness across the merged shadow DOM.
     *
     * All internal <a href> links are rewritten to plain #id anchors at parse time,
     * so no URI resolution is needed at navigation time.
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

        // Map of OPF-relative href → manifestId, used to resolve cross-chapter navigation links.
        // e.g. "xhtml/chapter1.xhtml" → "c001"
        val hrefToManifestId: Map<String, String> = manifest.values
            .associate { item -> item.attr("href") to item.id() }

        return spine.mapIndexedNotNull { index, itemref ->
            val idref = itemref.attr("idref")
            val item = manifest[idref] ?: return@mapIndexedNotNull null
            val href = item.attr("href")
            val chapterPath = resolvePath(opfDirectory, href)
            val rawContent = zip.readEpubEntryAsString(chapterPath) ?: ""
            val chapterDir = chapterPath.substringBeforeLast("/", "")

            Chapter(
                metadata = ChapterMetadata(
                    title = extractChapterTitle(rawContent),
                    id = idref,
                    chapterIndex = index
                ),
                content = rewriteUrls(
                    html = rawContent,
                    manifestId = idref,
                    chapterDir = chapterDir,
                    opfDirectory = opfDirectory,
                    filePath = filePath,
                    hrefToManifestId = hrefToManifestId
                )
            )
        }
    }

    /**
     * Rewrites all URLs and ids in the chapter HTML, in order:
     *
     *   1. rewriteResourceUrls    — asset refs (img/link/script/…) → epub:// scheme
     *   2. prefixInternalIds      — all [id] attributes → manifestId__id
     *   3. rewriteNavigationLinks — <a href> internal links → #manifestId or #manifestId__fragment
     *
     * Order matters: prefixInternalIds must run before rewriteNavigationLinks so that
     * local anchor links (#fragment) are rewritten consistently with the already-prefixed ids.
     */
    private fun rewriteUrls(
        html: String,
        manifestId: String,
        chapterDir: String,
        opfDirectory: String,
        filePath: String,
        hrefToManifestId: Map<String, String>
    ): String {
        val doc = Jsoup.parse(html)
        rewriteResourceUrls(doc, chapterDir, filePath)
        prefixInternalIds(doc, manifestId)
        rewriteNavigationLinks(doc, manifestId, chapterDir, opfDirectory, hrefToManifestId)
        return doc.toString()
    }

    /**
     * Rewrites resource references to the epub:// scheme so WebView can serve them
     * from the ZIP via shouldInterceptRequest.
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
    private fun rewriteResourceUrls(doc: Document, chapterDir: String, filePath: String) {
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
    }

    /**
     * Prefixes all [id] attributes in the chapter with the chapter's manifestId.
     *
     * This guarantees uniqueness across all chapters merged in the shadow DOM,
     * since EPUB only requires id uniqueness within a single file, not across files.
     *
     *   <h2 id="intro">  →  <h2 id="c001__intro">
     *   <p id="para-1">  →  <p id="c001__para-1">
     *
     * Must run before rewriteNavigationLinks so that local anchor hrefs (#fragment)
     * are rewritten consistently with the already-prefixed ids.
     */
    private fun prefixInternalIds(doc: Document, manifestId: String) {
        doc.select("[id]").forEach { el ->
            el.attr("id", "${manifestId}__${el.id()}")
        }
    }

    /**
     * Rewrites internal <a href> navigation links to plain #id anchors.
     *
     * Four cases:
     *
     *   1. Cross-chapter link with fragment:
     *      <a href="chapter2.xhtml#intro">  →  <a href="#c002__intro">
     *
     *   2. Cross-chapter link without fragment (link to chapter start):
     *      <a href="chapter2.xhtml">        →  <a href="#c002">
     *
     *   3. Local anchor (same chapter):
     *      <a href="#intro">                →  <a href="#c001__intro">
     *      Prefixed with current chapter's manifestId to match prefixInternalIds output.
     *
     *   4. Unresolvable or external link: left unchanged.
     *
     * After this rewrite, the JS click handler only needs:
     *   if href.startsWith('#') → navigateToId(href.slice(1))
     */
    private fun rewriteNavigationLinks(
        doc: Document,
        currentManifestId: String,
        chapterDir: String,
        opfDirectory: String,
        hrefToManifestId: Map<String, String>
    ) {
        doc.select("a[href]").forEach { anchor ->
            val href = anchor.attr("href")

            if (href.isAbsoluteUri() || href.isExternalScheme()) return@forEach

            val hrefPath = href.substringBefore('#')
            val fragment = href.substringAfter('#', "").takeIf { '#' in href }

            // Case 3: pure local anchor — prefix with current chapter's manifestId
            if (hrefPath.isEmpty() && fragment != null) {
                anchor.attr("href", "#${currentManifestId}__${fragment}")
                return@forEach
            }

            // Resolve the href path to an OPF-relative path for the manifest lookup
            val absolutePath = normalizePath(resolvePath(chapterDir, hrefPath))
            val opfRelativePath = if (opfDirectory.isEmpty()) absolutePath
            else absolutePath.removePrefix("$opfDirectory/")

            val targetManifestId = hrefToManifestId[opfRelativePath] ?: return@forEach

            anchor.attr("href", when {
                // Case 1: cross-chapter with fragment
                fragment != null -> "#${targetManifestId}__${fragment}"
                // Case 2: cross-chapter without fragment
                else             -> "#${targetManifestId}"
            })
        }
    }

    private fun String.isAbsoluteUri(): Boolean =
        startsWith("epub://")
                || startsWith("data:")
                || startsWith("http:")
                || startsWith("https:")
                || startsWith("//")
                || isEmpty()

    private fun String.isExternalScheme(): Boolean =
        startsWith("mailto:") || startsWith("tel:")

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