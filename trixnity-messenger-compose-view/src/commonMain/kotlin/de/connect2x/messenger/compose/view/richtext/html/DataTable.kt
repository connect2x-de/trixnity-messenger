package org.example.project.richtext

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.github.windedge.table.ColumnBuilder
import io.github.windedge.table.DataTable
import io.github.windedge.table.RowBuilderImpl
import io.github.windedge.table.RowsBuilder

@Composable
internal fun DataTable(
    node: RichText.Block,
    context: RichTextContext,
    modifier: Modifier = Modifier,
) {
    DataTable(
        columns = {
            for (node in node.children) {
                TableHeaderContent(node, context)
            }
        },
        footer = {
            for (node in node.children) {
                TableFooterContent(node, context)
            }
        },
        modifier = modifier,
    ) {
        for (node in node.children) {
            TableBodyContent(node, context)
        }
    }
}

internal fun ColumnBuilder.TableHeaderContent(node: RichText, context: RichTextContext) {
    when (node) {
        is RichText.Block -> TableHeaderContent(node, context)
        is RichText.InlineSpan -> Unit
    }
}

internal fun ColumnBuilder.TableHeaderContent(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "thead", "tr" -> {
            for (child in node.children) {
                TableHeaderContent(child, context)
            }
        }
        "th" -> {
            column {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Companion.Bold,
                    )
                ) {
                    Column {
                        for (index in node.children.indices) {
                            val child = node.children[index]
                            when (child) {
                                is RichText.Block -> BlockContent(child, context)
                                is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
                            }
                        }
                    }
                }
            }
        }
        "td" -> {
            column {
                Column {
                    for (index in node.children.indices) {
                        val child = node.children[index]
                        when (child) {
                            is RichText.Block -> BlockContent(child, context)
                            is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
                        }
                    }
                }
            }
        }
    }
}

internal fun RowsBuilder.TableBodyContent(node: RichText, context: RichTextContext) {
    when (node) {
        is RichText.Block -> TableBodyContent(node, context)
        is RichText.InlineSpan -> Unit
    }
}

internal fun RowsBuilder.TableBodyContent(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "tbody" -> {
            for (child in node.children) {
                TableBodyContent(child, context)
            }
        }
        "tr" -> {
            row {
                for (child in node.children) {
                    TableRowContent(child, context)
                }
            }
        }
    }
}

internal fun RowBuilderImpl.TableRowContent(node: RichText, context: RichTextContext) {
    when (node) {
        is RichText.Block -> TableRowContent(node, context)
        is RichText.InlineSpan -> Unit
    }
}

internal fun RowBuilderImpl.TableRowContent(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "th" -> {
            cell {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontWeight = FontWeight.Companion.Bold,
                    )
                ) {
                    Column {
                        for (index in node.children.indices) {
                            val child = node.children[index]
                            when (child) {
                                is RichText.Block -> BlockContent(child, context)
                                is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
                            }
                        }
                    }
                }
            }
        }
        "td" -> {
            cell {
                Column {
                    for (index in node.children.indices) {
                        val child = node.children[index]
                        when (child) {
                            is RichText.Block -> BlockContent(child, context)
                            is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.TableFooterContent(node: RichText, context: RichTextContext) {
    when (node) {
        is RichText.Block -> TableFooterContent(node, context)
        is RichText.InlineSpan -> Unit
    }
}

@Composable
internal fun BoxScope.TableFooterContent(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "thead", "tbody" -> {
            for (child in node.children) {
                TableFooterContent(child, context)
            }
        }
        "caption" -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BlockContent(node, context)
            }
        }
    }
}
