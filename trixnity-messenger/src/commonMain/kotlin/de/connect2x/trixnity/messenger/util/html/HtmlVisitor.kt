package de.connect2x.trixnity.messenger.util.html

import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.parser.Parser

class HtmlVisitor {
    private val taskQueue = mutableListOf<Task>()

    private data class Task(
        val node: Node,
        val acc: MutableList<HtmlNode>,
    )

    fun process(nodes: List<Node>): HtmlNode.HtmlElement {
        val children = mutableListOf<HtmlNode>()
        for (child in nodes) {
            visit(child, children)
        }
        var task: Task? = taskQueue.removeFirstOrNull()
        while (task != null) {
            visit(task.node, task.acc)
            task = taskQueue.removeFirstOrNull()
        }
        return HtmlNode.HtmlElement(
            tag = "#root",
            attributes = emptyMap(),
            children = children,
        )
    }

    private tailrec fun visit(node: Node, acc: MutableList<HtmlNode>) {
        when (node) {
            is TextNode -> {
                acc.add(
                    HtmlNode.TextContent(
                        content = node.text(),
                        rawContent = node.getWholeText(),
                    )
                )
            }
            is Element -> {
                val children = mutableListOf<HtmlNode>()
                acc.add(
                    HtmlNode.HtmlElement(
                        tag = node.tagName().lowercase(),
                        attributes = node.attributes().associate { it.key.lowercase() to it.value },
                        children = children,
                    )
                )
                val singleChild = node.childNodes.singleOrNull()
                if (singleChild != null) {
                    return visit(singleChild, children)
                }
                for (child in node.childNodes) {
                    taskQueue.add(Task(child, children))
                }
            }
        }
    }

    companion object {
        fun process(nodes: List<Node>): HtmlNode.HtmlElement =
            HtmlVisitor().process(nodes)

        fun process(document: String): HtmlNode.HtmlElement =
            HtmlVisitor().process(parse(document))

        private fun parse(document: String): List<Node> =
            Parser.parseFragment(document, Element("body"), "")
    }
}
