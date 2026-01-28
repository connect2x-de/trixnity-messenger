package de.connect2x.messenger.compose.view.richtext.html

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import de.connect2x.messenger.compose.view.common.HorizontalScrollableMeasurePolicy
import de.connect2x.messenger.compose.view.richtext.Children
import de.connect2x.messenger.compose.view.richtext.RichText
import de.connect2x.messenger.compose.view.richtext.RichTextContext
import de.connect2x.messenger.compose.view.richtext.em
import de.connect2x.messenger.compose.view.richtext.table.DataTable
import kotlin.time.ExperimentalTime

private val log: Logger = Logger("de.connect2x.messenger.compose.view.richtext.html.TableContentKt")

@OptIn(ExperimentalTime::class)
@Composable
internal fun TableContent(
    node: RichText.Block,
    context: RichTextContext,
) {
    val table = remember(node) {
        tryParseTable(node)
    }

    if (table != null) {
        val scrollState = rememberScrollState()
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.small,
        ) {
            Layout(
                content = {
                    DataTable(
                        columns = {
                            for (header in table.headers) {
                                column {
                                    TableCell(header, context)
                                }
                            }
                        },
                        footer = if (table.captions.isEmpty()) null else {
                            @Composable {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    for (caption in table.captions) {
                                        TableCaption(caption, context)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                            .padding(8.dp)
                    ) {
                        for (row in table.rows) {
                            row {
                                for (cell in row) {
                                    cell {
                                        TableCell(cell, context)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalScrollbar(
                        Modifier.layoutId(HorizontalScrollableMeasurePolicy.ScrollbarLayoutId),
                        scrollState
                    )
                },
                measurePolicy = HorizontalScrollableMeasurePolicy
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
            Children(node, context)
        }
    }
}

private fun validateTable(table: TableContent?): Boolean {
    if (table == null) return false
    if (table.headers.isEmpty()) return false
    if (!table.rows.fastAll { it.size == table.headers.size }) return false
    return true
}

private fun tryParseTable(node: RichText.Block): TableContent? =
    try {
        val table = parseTable(node).let { table ->
            if (table.headers.isEmpty() && table.rows.isNotEmpty()) {
                table.copy(
                    headers = table.rows.first(),
                    rows = table.rows.drop(1),
                )
            } else table
        }
        if (validateTable(table)) table else null
    } catch (e: Exception) {
        log.warn { "Could not parse table: $e" }
        null
    }
