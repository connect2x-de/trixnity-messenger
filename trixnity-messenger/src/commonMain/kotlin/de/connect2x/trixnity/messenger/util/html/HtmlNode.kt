package de.connect2x.trixnity.messenger.util.html

sealed interface HtmlNode {
    data class HtmlElement(
        val tag: String,
        val attributes: Map<String, String>,
        val children: List<HtmlNode>,
        val rawContent: String? = null,
    ) : HtmlNode

    data class TextContent(
        val content: String,
        val rawContent: String? = null,
    ) : HtmlNode
}
