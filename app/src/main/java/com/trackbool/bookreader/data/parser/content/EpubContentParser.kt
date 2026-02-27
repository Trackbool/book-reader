package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.ChapterMetadata
import com.trackbool.bookreader.domain.model.DocumentContent
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class EpubContentParser : DocumentContentParser {

    private var cachedFiles: Map<String, ByteArray>? = null
    private var cachedOpfDirectory: String? = null

    override fun parse(file: File): DocumentContent? {
        return try {
            parseEpub(file)
        } catch (e: Exception) {
            null
        }
    }

    override fun loadChapterContent(file: File, chapterIndex: Int): String {
        ensureFileLoaded(file)

        val files = cachedFiles ?: return ""
        val opfDirectory = cachedOpfDirectory ?: return ""

        val (spine, manifest) = loadSpineAndManifest(files) ?: return ""

        val itemref = spine.getOrNull(chapterIndex) ?: return ""
        val idref = itemref.attr("idref")
        val manifestItem = manifest[idref] ?: return ""

        val href = manifestItem.attr("href")
        val chapterPath = if (opfDirectory.isNotEmpty()) {
            "$opfDirectory/$href"
        } else {
            href
        }.replace("//", "/")

        return files[chapterPath]?.toString(Charsets.UTF_8) ?: ""
    }

    override fun supports(fileType: BookFileType): Boolean {
        return fileType == BookFileType.EPUB
    }

    private fun ensureFileLoaded(file: File) {
        if (cachedFiles != null) return

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

        val containerXml = files["META-INF/container.xml"] ?: return
        val opfPath = extractOpfPath(containerXml.toString(Charsets.UTF_8)) ?: return

        cachedFiles = files
        cachedOpfDirectory = opfPath.substringBeforeLast("/", "")
    }

    private fun parseEpub(file: File): DocumentContent {
        ensureFileLoaded(file)

        val files = cachedFiles ?: return DocumentContent(chapters = emptyList())

        val containerXml = files["META-INF/container.xml"]
            ?: return DocumentContent(chapters = emptyList())

        val opfPath = extractOpfPath(containerXml.toString(Charsets.UTF_8))
            ?: return DocumentContent(chapters = emptyList())

        val opfContent = files[opfPath]
            ?: return DocumentContent(chapters = emptyList())

        val opfDirectory = opfPath.substringBeforeLast("/", "")
        cachedOpfDirectory = opfDirectory

        val chapters = parseChapters(opfContent.toString(Charsets.UTF_8), files, opfDirectory)
        val language = extractLanguage(opfContent.toString(Charsets.UTF_8))

        return DocumentContent(
            chapters = chapters,
            language = language
        )
    }

    private fun loadSpineAndManifest(files: Map<String, ByteArray>): Pair<List<org.jsoup.nodes.Element>, Map<String, org.jsoup.nodes.Element>>? {
        val containerXml = files["META-INF/container.xml"] ?: return null
        val opfPath = extractOpfPath(containerXml.toString(Charsets.UTF_8)) ?: return null
        val opfContent = files[opfPath] ?: return null

        val doc = Jsoup.parse(opfContent.toString(Charsets.UTF_8), "", Parser.xmlParser())
        val spine = doc.select("spine itemref")
        val manifest = doc.select("manifest item").associateBy { it.id() }

        return Pair(spine, manifest)
    }

    private fun extractOpfPath(containerXml: String): String? {
        val doc = Jsoup.parse(containerXml, "", Parser.xmlParser())
        val rootFile = doc.selectFirst("rootfile")
        return rootFile?.attr("full-path")
    }

    private fun extractLanguage(opfContent: String): String? {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())
        return doc.selectFirst("dc|language, language")?.text()
    }

    private fun parseChapters(
        opfContent: String,
        files: Map<String, ByteArray>,
        opfDirectory: String
    ): List<ChapterMetadata> {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())

        val spine = doc.select("spine itemref")
        val manifest = doc.select("manifest item").associateBy { it.id() }

        val chapters = mutableListOf<ChapterMetadata>()

        spine.forEachIndexed { index, itemref ->
            val idref = itemref.attr("idref")
            val manifestItem = manifest[idref] ?: return@forEachIndexed

            val href = manifestItem.attr("href")
            val chapterPath = if (opfDirectory.isNotEmpty()) {
                "$opfDirectory/$href"
            } else {
                href
            }.replace("//", "/")

            val chapterContent = files[chapterPath]?.toString(Charsets.UTF_8) ?: ""
            val title = extractChapterTitle(chapterContent, index)

            chapters.add(
                ChapterMetadata(
                    title = title,
                    chapterIndex = index,
                    href = href
                )
            )
        }

        return chapters
    }

    private fun extractChapterTitle(htmlContent: String, fallbackIndex: Int): String {
        val doc = Jsoup.parse(htmlContent)
        val title = doc.selectFirst("title")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: doc.selectFirst("h2")?.text()

        return title?.takeIf { it.isNotBlank() } ?: "Capítulo ${fallbackIndex + 1}"
    }
}
