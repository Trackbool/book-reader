package com.trackbool.bookreader.ui.parser

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.trackbool.bookreader.ui.model.ReaderContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import javax.inject.Inject

class EpubContentRenderParser @Inject constructor() : BookContentRenderParser {

    private class BuilderRef(var value: AnnotatedString.Builder = AnnotatedString.Builder())

    override suspend fun parse(text: String): List<ReaderContent> {
        return withContext(Dispatchers.IO) {
            try {
                parseEpubText(text)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseEpubText(text: String): List<ReaderContent> {
        val result = mutableListOf<ReaderContent>()
        val document = Jsoup.parse(text)
        val body = document.body() ?: return emptyList()
        parseBlockElements(body, result)
        return result
    }

    private fun parseBlockElements(
        element: Element,
        result: MutableList<ReaderContent>
    ) {
        element.childNodes().forEach { node ->
            when (node) {
                is Element -> when (node.tagName().lowercase()) {

                    "p" -> {
                        val items = mutableListOf<ReaderContent>()
                        val builderRef = BuilderRef()
                        collectMixedContent(node, builderRef, items, headingStyle = null)
                        flushBuilder(builderRef.value, items)
                        result.addAll(items)
                    }

                    "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        val items = mutableListOf<ReaderContent>()
                        val builderRef = BuilderRef()
                        val headingStyle = headingStyleFor(node.tagName().lowercase())
                        builderRef.value.pushStyle(headingStyle)
                        collectMixedContent(node, builderRef, items, headingStyle = headingStyle)
                        flushBuilder(builderRef.value, items)
                        result.addAll(items)
                    }

                    "div", "section", "article", "body" -> {
                        parseBlockElements(node, result)
                    }

                    "br" -> {
                        result.add(ReaderContent.Text(AnnotatedString("\n")))
                    }

                    "img" -> {
                        val src = node.attr("src")
                        if (src.isNotBlank()) {
                            result.add(
                                ReaderContent.Image(
                                    src = src,
                                    alt = node.attr("alt").ifBlank { null }
                                )
                            )
                        }
                    }

                    else -> parseBlockElements(node, result)
                }

                is TextNode -> {
                    val text = node.text().trim()
                    if (text.isNotBlank()) {
                        result.add(ReaderContent.Text(AnnotatedString(text)))
                    }
                }
            }
        }
    }

    /**
     * Recursively walks the node tree collecting text (with inline styles)
     * into [builderRef] and emitting ReaderContent.Image whenever an <img> is
     * encountered at any nesting depth.
     *
     * When an image is found, the text accumulated so far is flushed as a
     * Text item before emitting the Image, so ordering is preserved:
     *
     *   <p>Intro <span><img src="..."/></span> conclusion</p>
     *   -> [Text("Intro "), Image(...), Text("conclusion\n")]
     *
     * [headingStyle] is re-applied on each fresh builder after a flush so that
     * text segments following an image inside a heading retain correct styling.
     */
    private fun collectMixedContent(
        node: Node,
        builderRef: BuilderRef,
        result: MutableList<ReaderContent>,
        headingStyle: SpanStyle?
    ) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) builderRef.value.append(text)
            }

            is Element -> when (node.tagName().lowercase()) {
                "img" -> {
                    builderRef.value = flushBuilder(builderRef.value, result, headingStyle)
                    val src = node.attr("src")
                    if (src.isNotBlank()) {
                        result.add(
                            ReaderContent.Image(
                                src = src,
                                alt = node.attr("alt").ifBlank { null }
                            )
                        )
                    }
                }

                "em", "i" -> {
                    builderRef.value.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    node.childNodes().forEach { collectMixedContent(it, builderRef, result, headingStyle) }
                    builderRef.value.pop()
                }

                "strong", "b" -> {
                    builderRef.value.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    node.childNodes().forEach { collectMixedContent(it, builderRef, result, headingStyle) }
                    builderRef.value.pop()
                }

                "u" -> {
                    builderRef.value.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    node.childNodes().forEach { collectMixedContent(it, builderRef, result, headingStyle) }
                    builderRef.value.pop()
                }

                "s", "strike", "del" -> {
                    builderRef.value.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    node.childNodes().forEach { collectMixedContent(it, builderRef, result, headingStyle) }
                    builderRef.value.pop()
                }

                "br" -> builderRef.value.append("\n")

                // span, a, and other inline wrappers — transparent, keep descending
                else -> node.childNodes().forEach { collectMixedContent(it, builderRef, result, headingStyle) }
            }
        }
    }

    /**
     * Emits the current builder content as a Text item with a trailing newline.
     * Returns a fresh builder to replace the old one, with [headingStyle]
     * re-applied if present so subsequent text segments retain heading styling.
     * No-op if the accumulated text is blank.
     */
    private fun flushBuilder(
        builder: AnnotatedString.Builder,
        result: MutableList<ReaderContent>,
        headingStyle: SpanStyle? = null
    ): AnnotatedString.Builder {
        val annotated = builder.toAnnotatedString()
        if (annotated.text.isNotBlank()) {
            result.add(ReaderContent.Text(annotated + AnnotatedString("\n")))
        }
        return AnnotatedString.Builder().apply {
            if (headingStyle != null) pushStyle(headingStyle)
        }
    }

    private fun headingStyleFor(tag: String): SpanStyle = when (tag) {
        "h1" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp)
        "h2" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)
        "h3" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
        else -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}