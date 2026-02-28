package com.trackbool.bookreader.data.parser.content

import com.trackbool.bookreader.data.parser.extractOpfPath
import com.trackbool.bookreader.data.parser.readEpubEntryAsString
import com.trackbool.bookreader.data.parser.resolvePath
import com.trackbool.bookreader.domain.model.BookFileType
import com.trackbool.bookreader.domain.model.ChapterMetadata
import com.trackbool.bookreader.domain.model.DocumentContent
import com.trackbool.bookreader.domain.parser.content.DocumentContentParser
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

class EpubContentParser : DocumentContentParser {

    override fun parse(file: File): DocumentContent? {
        return try {
            ZipFile(file).use { zip -> parseEpub(zip) }
        } catch (e: Exception) {
            null
        }
    }

    override fun loadChapterContent(file: File, chapterIndex: Int): String {
        return try {
            ZipFile(file).use { zip -> readChapterContent(zip, chapterIndex) }
        } catch (e: Exception) {
            ""
        }
    }

    override fun supports(fileType: BookFileType) = fileType == BookFileType.EPUB

    private fun parseEpub(zip: ZipFile): DocumentContent {
        val opfPath = zip.extractOpfPath() ?: return DocumentContent(chapters = emptyList())
        val opfContent = zip.readEpubEntryAsString(opfPath) ?: return DocumentContent(chapters = emptyList())
        val opfDirectory = opfPath.substringBeforeLast("/", "")

        return DocumentContent(
            chapters = parseChapters(opfDirectory, opfContent, zip),
            language = extractLanguage(opfContent)
        )
    }

    private fun readChapterContent(zip: ZipFile, chapterIndex: Int): String {
        val opfPath = zip.extractOpfPath() ?: return ""
        val opfContent = zip.readEpubEntryAsString(opfPath) ?: return ""
        val opfDirectory = opfPath.substringBeforeLast("/", "")

        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())
        val spine = doc.select("spine itemref")
        val manifest = doc.select("manifest item").associateBy { it.id() }

        val idref = spine.getOrNull(chapterIndex)?.attr("idref") ?: return ""
        val href = manifest[idref]?.attr("href") ?: return ""

        return zip.readEpubEntryAsString(resolvePath(opfDirectory, href)) ?: ""
    }

    private fun parseChapters(
        opfDirectory: String,
        opfContent: String,
        zip: ZipFile
    ): List<ChapterMetadata> {
        val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())
        val spine = doc.select("spine itemref")
        val manifest = doc.select("manifest item").associateBy { it.id() }

        return spine.mapIndexedNotNull { index, itemref ->
            val idref = itemref.attr("idref")
            val href = manifest[idref]?.attr("href") ?: return@mapIndexedNotNull null

            val chapterContent = zip.readEpubEntryAsString(resolvePath(opfDirectory, href)) ?: ""
            ChapterMetadata(
                title        = extractChapterTitle(chapterContent),
                chapterIndex = index,
                href         = href
            )
        }
    }

    private fun extractLanguage(opfContent: String): String? {
        return Jsoup.parse(opfContent, "", Parser.xmlParser())
            .selectFirst("dc|language, language")?.text()
    }

    private fun extractChapterTitle(htmlContent: String): String? {
        val doc = Jsoup.parse(htmlContent)
        return doc.selectFirst("title")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h1")?.text()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("h2")?.text()?.takeIf { it.isNotBlank() }
    }
}