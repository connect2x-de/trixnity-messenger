package de.connect2x.trixnity.messenger.util.html

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
                children.add(HtmlNode.TextContent(node.content.substring(index, match.result.range.start)))
                children.add(linkElement(match))
                index = match.result.range.last + 1
                match = matcher.next(index)
            }
            children.add(HtmlNode.TextContent(node.content.substring(index)))
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

    private fun linkElement(match: LinkMatcher.Match): HtmlNode.HtmlElement = when (match.type) {
        LinkMatcher.MatchType.URL ->
            HtmlNode.HtmlElement(
                tag = "a",
                attributes = mapOf("href" to match.result.value),
                children = listOf(HtmlNode.TextContent(match.result.value)),
            )
        LinkMatcher.MatchType.USER_ID ->
            HtmlNode.HtmlElement(
                tag = "a",
                attributes = mapOf("href" to "matrix:u/${match.result.value.trimStart('@')}"),
                children = listOf(HtmlNode.TextContent(match.result.value)),
            )
        LinkMatcher.MatchType.ROOM_ALIAS ->
            HtmlNode.HtmlElement(
                tag = "a",
                attributes = mapOf("href" to "matrix:r/${match.result.value.trimStart('#')}"),
                children = listOf(HtmlNode.TextContent(match.result.value)),
            )
    }

    companion object {
        fun process(document: HtmlNode.HtmlElement): HtmlNode.HtmlElement =
            AutoLinkifyVisitor().process(document)
    }
}
