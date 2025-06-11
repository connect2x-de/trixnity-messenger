package org.example.project.html

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

sealed interface HtmlNode {
    data class HtmlElement(val tag: String, val attributes: Map<String, String>, val children: List<HtmlNode>) :
        HtmlNode

    data class TextContent(val fullContent: String, val content: String) : HtmlNode
}

@Composable
inline fun rememberHtml(content: String): HtmlNode.HtmlElement =
    remember(content) { parseHtml(content) }

@PublishedApi
internal fun parseHtml(content: String): HtmlNode.HtmlElement =
    parseHtml(Ksoup.parse(content))

@PublishedApi
internal fun parseHtml(content: Node): HtmlNode? = when (content) {
    is TextNode -> parseHtml(content)
    is Element -> parseHtml(content)
    else -> null
}

@PublishedApi
internal fun parseHtml(content: TextNode): HtmlNode.TextContent =
    HtmlNode.TextContent(content.getWholeText(), content.text())

@PublishedApi
internal fun parseHtml(content: Element): HtmlNode.HtmlElement =
    HtmlNode.HtmlElement(
        tag = content.tagName(),
        attributes = content.attributes().associate { it.key to it.value },
        children = content.childNodes.mapNotNull(::parseHtml),
    )