package com.trackbool.bookreader.data.parser.text

import com.trackbool.bookreader.domain.parser.text.ReaderText
import com.trackbool.bookreader.domain.parser.text.TextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

class EpubTextParser : TextParser {

    override suspend fun parse(text: String): List<ReaderText> {
        return withContext(Dispatchers.IO) {
            try {
                parseBookContent(text)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun parseBookContent(text: String): List<ReaderText> {
        val result = mutableListOf<ReaderText>()
        val document = Jsoup.parse(text)
        val body = document.body() ?: return emptyList()

        parseBlockElements(body, result)

        return result
    }

    private fun parseBlockElements(
        element: Element,
        result: MutableList<ReaderText>
    ) {
        element.childNodes().forEach { node ->
            when (node) {

                is Element -> {
                    when (node.tagName().lowercase()) {

                        "p" -> {
                            val annotated = buildAnnotatedStringFromElement(node)
                            if (annotated.text.isNotBlank()) {
                                result.add(ReaderText.Text(annotated))
                            }
                        }

                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            val annotated = buildAnnotatedStringFromElement(
                                node,
                                heading = true
                            )
                            if (annotated.text.isNotBlank()) {
                                result.add(ReaderText.Text(annotated))
                            }
                        }

                        "div", "section", "article", "body" -> {
                            parseBlockElements(node, result)
                        }

                        "br" -> {
                            result.add(
                                ReaderText.Text(
                                    AnnotatedString("\n")
                                )
                            )
                        }

                        "img" -> {
                            val src = node.attr("src")
                            if (src.isNotBlank()) {
                                result.add(
                                    ReaderText.Image(
                                        src = src,
                                        alt = node.attr("alt").ifBlank { null }
                                    )
                                )
                            }
                        }

                        else -> {
                            parseBlockElements(node, result)
                        }
                    }
                }

                is TextNode -> {
                    val text = node.text().trim()
                    if (text.isNotBlank()) {
                        result.add(
                            ReaderText.Text(
                                AnnotatedString(text)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildAnnotatedStringFromElement(
        element: Element,
        heading: Boolean = false
    ): AnnotatedString {

        val builder = AnnotatedString.Builder()

        if (heading) {
            builder.pushStyle(
                SpanStyle(fontWeight = FontWeight.Bold)
            )
        }

        element.childNodes().forEach { node ->
            parseInlineNode(node, builder)
        }

        if (heading) {
            builder.pop()
        }

        return builder.toAnnotatedString()
    }

    private fun parseInlineNode(
        node: Node,
        builder: AnnotatedString.Builder
    ) {
        when (node) {

            is TextNode -> {
                builder.append(node.text())
            }

            is Element -> {

                when (node.tagName().lowercase()) {

                    "em", "i" -> {
                        builder.pushStyle(
                            SpanStyle(fontStyle = FontStyle.Italic)
                        )
                        node.childNodes().forEach {
                            parseInlineNode(it, builder)
                        }
                        builder.pop()
                    }

                    "strong", "b" -> {
                        builder.pushStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        )
                        node.childNodes().forEach {
                            parseInlineNode(it, builder)
                        }
                        builder.pop()
                    }

                    "u" -> {
                        builder.pushStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline)
                        )
                        node.childNodes().forEach {
                            parseInlineNode(it, builder)
                        }
                        builder.pop()
                    }

                    "s", "strike", "del" -> {
                        builder.pushStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough)
                        )
                        node.childNodes().forEach {
                            parseInlineNode(it, builder)
                        }
                        builder.pop()
                    }

                    "br" -> {
                        builder.append("\n")
                    }

                    else -> {
                        node.childNodes().forEach {
                            parseInlineNode(it, builder)
                        }
                    }
                }
            }
        }
    }
}