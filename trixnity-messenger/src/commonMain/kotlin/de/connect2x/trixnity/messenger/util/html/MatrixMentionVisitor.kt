package de.connect2x.trixnity.messenger.util.html

import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.util.MatrixLinks

class MatrixMentionVisitor {
    private val taskQueue = mutableListOf<HtmlNode>()
    private val mentions = mutableMapOf<String, Mention>()

    fun process(node: HtmlNode): Map<String, Mention> {
        visit(node)
        var task: HtmlNode? = taskQueue.removeFirstOrNull()
        while (task != null) {
            visit(task)
            task = taskQueue.removeFirstOrNull()
        }
        return mentions
    }

    private fun visit(node: HtmlNode) {
        when (node) {
            is HtmlNode.TextContent -> visit(node)
            is HtmlNode.HtmlElement -> visit(node)
        }
    }

    private fun visit(node: HtmlNode.TextContent) = Unit

    private fun visit(node: HtmlNode.HtmlElement) {
        if (node.tag == "a") {
            val href = node.attributes["href"]
            if (href != null) {
                if (!mentions.containsKey(href)) {
                    val link = MatrixLinks.parse(href)
                    if (link != null) {
                        mentions.put(href, link)
                    }
                }
                return
            }
        }
        for (child in node.children) {
            taskQueue.add(child)
        }
    }

    companion object {
        fun process(document: HtmlNode.HtmlElement): HtmlNode.HtmlElement =
            AutoLinkifyVisitor().process(document)
    }
}
