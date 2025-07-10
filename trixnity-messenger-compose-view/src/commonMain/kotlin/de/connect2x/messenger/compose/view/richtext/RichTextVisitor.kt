package de.connect2x.messenger.compose.view.richtext

import de.connect2x.trixnity.messenger.util.html.HtmlNode

class RichTextVisitor {
    private val taskQueue = mutableListOf<Task>()

    private sealed interface Task {
        data class Block(
            val node: HtmlNode.HtmlElement,
            val acc: MutableList<RichText>,
        ): Task
        data class Inline(
            val node: HtmlNode,
            val acc: MutableList<RichText.Inline>,
        ): Task
        data class InlineSpan(
            val node: List<RichText.Inline>,
            val acc: MutableList<RichText>,
        ): Task
    }

    fun process(document: HtmlNode.HtmlElement): RichText.Block {
        val children = mutableListOf<RichText>()
        val singleChild = document.children.singleOrNull()
        if (singleChild is HtmlNode.HtmlElement && singleChild.tag !in RichText.inline) {
            visitBlock(singleChild, children)
        } else {
            queueChildren(document.children, children)
        }
        var task: Task? = taskQueue.removeFirstOrNull()
        while (task != null) {
            when (task) {
                is Task.Block -> visitBlock(task.node, task.acc)
                is Task.Inline -> visitInline(task.node, task.acc)
                is Task.InlineSpan -> visitSpan(task.node, task.acc)
            }
            task = taskQueue.removeFirstOrNull()
        }
        return RichText.Block(
            tag = document.tag,
            attributes = document.attributes,
            children = children,
        )
    }

    private fun visitSpan(node: List<RichText.Inline>, acc: MutableList<RichText>) {
        acc.add(RichText.InlineSpan(node))
    }

    private fun visitInline(node: HtmlNode, acc: MutableList<RichText.Inline>) {
        when (node) {
            is HtmlNode.TextContent -> visitInline(node, acc)
            is HtmlNode.HtmlElement -> visitInline(node, acc)
        }
    }

    private fun visitInline(node: HtmlNode.TextContent, acc: MutableList<RichText.Inline>) {
        acc.add(RichText.Inline.Text(
            content = node.content,
            rawContent = node.rawContent,
        ))
    }

    private fun visitInline(node: HtmlNode.HtmlElement, acc: MutableList<RichText.Inline>) {
        val children = mutableListOf<RichText.Inline>()
        acc.add(RichText.Inline.Block(
            tag = node.tag.lowercase(),
            attributes = node.attributes,
            rawContent = node.rawContent,
            children = children,
        ))
        for (child in node.children) {
            taskQueue.add(Task.Inline(child, children))
        }
    }

    private tailrec fun visitBlock(node: HtmlNode.HtmlElement, acc: MutableList<RichText>) {
        val children = mutableListOf<RichText>()
        acc.add(
            RichText.Block(
                tag = node.tag,
                attributes = node.attributes,
                children = children,
            )
        )
        val singleChild = node.children.singleOrNull()
        if (singleChild is HtmlNode.HtmlElement && singleChild.tag !in RichText.inline) {
            return visitBlock(singleChild, children)
        } else {
            queueChildren(node.children, children)
        }
    }

    private fun queueChildren(nodes: List<HtmlNode>, acc: MutableList<RichText>) {
        var inlineAcc: MutableList<RichText.Inline>? = null
        for (child in nodes) {
            when (child) {
                is HtmlNode.TextContent -> {
                    if (inlineAcc == null) { inlineAcc = mutableListOf() }
                    taskQueue.add(Task.Inline(child, inlineAcc))
                }

                is HtmlNode.HtmlElement -> {
                    if (child.tag in RichText.inline) {
                        if (inlineAcc == null) { inlineAcc = mutableListOf() }
                        taskQueue.add(Task.Inline(child, inlineAcc))
                    } else {
                        if (inlineAcc != null) {
                            taskQueue.add(Task.InlineSpan(inlineAcc, acc))
                            inlineAcc = null
                        }
                        taskQueue.add(Task.Block(child, acc))
                    }
                }
            }
        }
        if (inlineAcc != null) {
            taskQueue.add(Task.InlineSpan(inlineAcc, acc))
        }
    }

    companion object {
        fun process(document: HtmlNode.HtmlElement): RichText.Block =
            RichTextVisitor().process(document)
    }
}
