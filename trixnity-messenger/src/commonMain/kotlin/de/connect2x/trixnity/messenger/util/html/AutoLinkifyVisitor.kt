package de.connect2x.trixnity.messenger.util.html

import net.folivo.trixnity.core.util.Reference
import net.folivo.trixnity.core.util.References

class AutoLinkifyVisitor {
    private val taskQueue = mutableListOf<Task>()

    private data class Task(
        val node: HtmlNode,
        val acc: MutableList<HtmlNode>,
    )

    fun process(document: HtmlNode.HtmlElement): HtmlNode.HtmlElement {
        val children = mutableListOf<HtmlNode>()
        for (child in document.children) {
            visit(child, children)
        }
        var task: Task? = taskQueue.removeFirstOrNull()
        while (task != null) {
            visit(task.node, task.acc)
            task = taskQueue.removeFirstOrNull()
        }
        return document.copy(children = children)
    }

    private fun visit(node: HtmlNode, acc: MutableList<HtmlNode>) {
        when (node) {
            is HtmlNode.TextContent -> visit(node, acc)
            is HtmlNode.HtmlElement -> visit(node, acc)
        }
    }

    private fun visit(node: HtmlNode.TextContent, acc: MutableList<HtmlNode>) {
        var index = 0
        val matches = References.findReferences(node.content)
        if (matches.isEmpty()) {
            acc.add(node)
        } else {
            val children = mutableListOf<HtmlNode>()
            acc.add(
                HtmlNode.HtmlElement(
                    tag = "span",
                    attributes = mapOf(),
                    rawContent = node.rawContent,
                    children = children,
                )
            )
            for (match in matches) {
                val previousContent = node.content.substring(index, match.key.first)
                if (previousContent.isNotEmpty()) {
                    children.add(HtmlNode.TextContent(previousContent))
                }
                children.add(linkElement(match.value))
                index = match.key.last + 1
            }
            val previousContent = node.content.substring(index)
            if (previousContent.isNotEmpty()) {
                children.add(HtmlNode.TextContent(previousContent))
            }
        }
    }

    private tailrec fun visit(node: HtmlNode.HtmlElement, acc: MutableList<HtmlNode>) {
        if (node.tag.lowercase() == "a" && !node.attributes["href"].isNullOrEmpty()) {
            acc.add(node)
        } else {
            val children = mutableListOf<HtmlNode>()
            acc.add(node.copy(children = children))
            val singleChild = node.children.singleOrNull()
            if (singleChild is HtmlNode.HtmlElement) {
                return visit(singleChild, children)
            } else {
                for (child in node.children) {
                    taskQueue.add(Task(child, children))
                }
            }
        }
    }

    private fun linkElement(match: Reference): HtmlNode.HtmlElement =
        HtmlNode.HtmlElement(
            tag = "a",
            attributes = mapOf("href" to (match.uri ?: match.toLink())),
            children = listOf(
                HtmlNode.TextContent(
                    when (match) {
                        is Reference.Event -> match.eventId.full
                        is Reference.Room -> match.roomId.full
                        is Reference.RoomAlias -> match.roomAliasId.full
                        is Reference.User -> match.userId.full
                        is Reference.Link -> match.uri
                    }
                )
            ),
        )

    companion object {
        fun process(document: HtmlNode.HtmlElement): HtmlNode.HtmlElement =
            AutoLinkifyVisitor().process(document)
    }
}
