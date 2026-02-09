package de.connect2x.trixnity.messenger.compose.view.richtext.html

import de.connect2x.trixnity.messenger.compose.view.richtext.RichText

internal data class TableContent(
    val headers: List<RichText>,
    val rows: List<List<RichText>>,
    val captions: List<RichText>,
)

internal fun parseTable(node: RichText.Block): TableContent =
    TableContent(
        headers = parseTableHeader(node),
        rows = parseTableRows(node),
        captions = parseTableCaption(node),
    )

internal fun parseTableHeader(node: RichText): List<RichText> =
    when (node) {
        is RichText.Block -> parseTableHeader(node)
        is RichText.InlineSpan -> emptyList()
    }

internal fun parseTableHeader(node: RichText.Block): List<RichText> =
    when (node.tag) {
        "thead" -> parseTableRow(node)
        else -> node.children.flatMap { parseTableHeader(it) }
    }

internal fun parseTableRows(node: RichText): List<List<RichText>> =
    when (node) {
        is RichText.Block -> parseTableRows(node)
        is RichText.InlineSpan -> emptyList()
    }

internal fun parseTableRows(node: RichText.Block): List<List<RichText>> {
    return when (node.tag) {
        "thead", "tfoot" -> emptyList()
        "tr" -> listOf(parseTableRow(node))
        else -> node.children.flatMap {
            parseTableRows(it)
        }
    }
}

internal fun parseTableRow(node: RichText): List<RichText> =
    when (node) {
        is RichText.Block -> parseTableRow(node)
        is RichText.InlineSpan -> emptyList()
    }

internal fun parseTableRow(node: RichText.Block): List<RichText> =
    when (node.tag) {
        "td", "th" -> listOf(node)
        else -> node.children.flatMap { parseTableRow(it) }
    }

internal fun parseTableCaption(node: RichText): List<RichText> =
    when (node) {
        is RichText.Block -> parseTableCaption(node)
        is RichText.InlineSpan -> emptyList()
    }

internal fun parseTableCaption(node: RichText.Block): List<RichText> =
    when (node.tag) {
        "caption" -> listOf(node)
        else -> node.children.flatMap { parseTableCaption(it) }
    }
