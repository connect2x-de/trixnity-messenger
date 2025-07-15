package de.connect2x.trixnity.messenger.util.html

import net.folivo.trixnity.core.model.Mention

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
        val matcher = LinkMatcher(node.content)
        var match: LinkMatcher.Match? = matcher.next(index)
        if (match == null) {
            acc.add(node)
        } else {
            val children = mutableListOf<HtmlNode>()
            acc.add(HtmlNode.HtmlElement(
                tag = "span",
                attributes = mapOf(),
                rawContent = node.rawContent,
                children = children,
            ))
            while (match != null) {
                val previousContent = node.content.substring(index, match.result.range.first)
                if (previousContent.isNotEmpty()) {
                    children.add(HtmlNode.TextContent(previousContent))
                }
                val content = match.result.value.trimParens().trimEnd('.', '!', '?', ':')
                children.add(linkElement(match.type, content))
                index = match.result.range.first + content.length
                match = matcher.next(index)
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

    private fun linkElement(type: LinkMatcher.MatchType, content: String): HtmlNode.HtmlElement = when (type) {
        LinkMatcher.MatchType.URL ->
            HtmlNode.HtmlElement(
                tag = "a",
                attributes = mapOf("href" to content),
                children = listOf(HtmlNode.TextContent(
                    when (val mention = MatrixMentionVisitor.parseLink(content)) {
                        is Mention.Event -> content
                        is Mention.Room -> mention.roomId.full
                        is Mention.RoomAlias -> mention.roomAliasId.full
                        is Mention.User -> mention.userId.full
                        null -> content
                    }
                )),
            )
        LinkMatcher.MatchType.USER_ID ->
            HtmlNode.HtmlElement(
                tag = "a",
                attributes = mapOf("href" to "matrix:u/${content.trimStart('@')}"),
                children = listOf(HtmlNode.TextContent(content)),
            )
        LinkMatcher.MatchType.ROOM_ALIAS ->
            HtmlNode.HtmlElement(
                tag = "a",
                attributes = mapOf("href" to "matrix:r/${content.trimStart('#')}"),
                children = listOf(HtmlNode.TextContent(content)),
            )
    }

    private fun String.trimParens(): String =
        if (endsWith(')')) {
            val trimmed = trimEnd(')')
            val openingParens = trimmed.count { it == '(' }
            val closingParens = trimmed.count { it == ')' }
            val endingParens = length - trimmed.length
            val openParens = openingParens - closingParens

            val desiredParens = minOf(endingParens, openParens)
            take(trimmed.length + desiredParens)
        } else this

    companion object {
        fun process(document: HtmlNode.HtmlElement): HtmlNode.HtmlElement =
            AutoLinkifyVisitor().process(document)
    }
}
