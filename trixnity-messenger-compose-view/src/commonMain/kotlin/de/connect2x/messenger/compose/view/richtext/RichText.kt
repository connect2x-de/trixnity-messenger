package de.connect2x.messenger.compose.view.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.util.html.HtmlNode

sealed interface RichText {
    data class Block(
        val tag: String,
        val attributes: Map<String, String>,
        val children: List<RichText>,
    ) : RichText

    data class InlineSpan(val children: List<Inline>) : RichText

    sealed interface Inline {
        data class Block(
            val tag: String,
            val attributes: Map<String, String>,
            val children: List<Inline>,
            val rawContent: String? = null,
        ) : Inline

        data class Text(
            val content: String,
            val rawContent: String? = null,
        ) : Inline
    }

    companion object {
        val inline = listOf(
            "a", "sup", "sub", "b", "i", "u", "strong", "em", "s", "code", "br", "span",
        )
    }
}

@Composable
inline fun rememberRichText(content: HtmlNode.HtmlElement): RichText.Block =
    remember(content) { parseRichText(content) }

@PublishedApi
internal fun parseRichText(document: HtmlNode.HtmlElement): RichText.Block = parseBlock(document)

@PublishedApi
internal fun parseBlock(html: HtmlNode.HtmlElement): RichText.Block =
    RichText.Block(tag = html.tag, attributes = html.attributes, children = buildList {
        val inlineNodes = mutableListOf<RichText.Inline>()
        for (child in html.children) {
            when (child) {
                is HtmlNode.TextContent -> {
                    inlineNodes.add(parseInline(child))
                }

                is HtmlNode.HtmlElement ->
                    if (child.tag in RichText.inline) {
                        inlineNodes.add(parseInline(child))
                    } else {
                        if (inlineNodes.isNotEmpty()) {
                            add(RichText.InlineSpan(inlineNodes.toList()))
                            inlineNodes.clear()
                        }
                        add(parseBlock(child))
                    }
            }
        }
        if (inlineNodes.isNotEmpty()) {
            add(RichText.InlineSpan(inlineNodes))
        }
    })

@PublishedApi
internal fun parseInline(html: HtmlNode): RichText.Inline =
    when (html) {
        is HtmlNode.HtmlElement -> parseInline(html)
        is HtmlNode.TextContent -> parseInline(html)
    }

@PublishedApi
internal fun parseInline(html: HtmlNode.HtmlElement): RichText.Inline.Block =
    RichText.Inline.Block(
        tag = html.tag.lowercase(),
        attributes = html.attributes,
        children = html.children.map { parseInline(it) },
        rawContent = html.rawContent,
    )

internal fun parseInline(html: HtmlNode.TextContent): RichText.Inline.Text =
    RichText.Inline.Text(
        content = html.content,
        rawContent = html.rawContent,
    )
