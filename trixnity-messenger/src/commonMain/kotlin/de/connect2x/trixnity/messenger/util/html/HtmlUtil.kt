package de.connect2x.trixnity.messenger.util.html

internal fun innerText(node: HtmlNode) =
    buildString {
        appendInnerText(node)
    }

private fun StringBuilder.appendInnerText(node: HtmlNode) {
    when (node) {
        is HtmlNode.HtmlElement -> appendInnerText(node)
        is HtmlNode.TextContent -> appendInnerText(node)
    }
}

private fun StringBuilder.appendInnerText(node: HtmlNode.TextContent) {
    append(node.content)
}

private fun StringBuilder.appendInnerText(node: HtmlNode.HtmlElement) {
    for (child in node.children) {
        appendInnerText(child)
    }
}
