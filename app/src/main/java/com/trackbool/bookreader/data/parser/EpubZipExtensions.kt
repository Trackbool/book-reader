package com.trackbool.bookreader.data.parser

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.nio.charset.Charset
import java.util.zip.ZipFile

internal fun ZipFile.readEpubEntryAsString(path: String): String? {
    val entry = getEntry(path) ?: return null
    val bytes = getInputStream(entry).use { it.readBytes() }
    val encoding = detectXmlEncoding(bytes) ?: Charsets.UTF_8
    return bytes.toString(encoding)
}

internal fun ZipFile.readEpubEntryAsBytes(path: String): ByteArray? {
    val entry = getEntry(path) ?: return null
    return getInputStream(entry).use { it.readBytes() }
}

internal fun ZipFile.extractOpfPath(): String? {
    val containerXml = readEpubEntryAsString("META-INF/container.xml") ?: return null
    return Jsoup.parse(containerXml, "", Parser.xmlParser())
        .selectFirst("rootfile")
        ?.attr("full-path")
}

internal fun resolvePath(directory: String, href: String): String {
    return if (directory.isNotEmpty()) "$directory/$href" else href
}

private fun detectXmlEncoding(bytes: ByteArray): Charset? {
    val header = bytes.take(200).toByteArray().toString(Charsets.US_ASCII)
    val match = Regex("""encoding=["']([^"']+)["']""").find(header) ?: return null
    return runCatching { charset(match.groupValues[1]) }.getOrNull()
}