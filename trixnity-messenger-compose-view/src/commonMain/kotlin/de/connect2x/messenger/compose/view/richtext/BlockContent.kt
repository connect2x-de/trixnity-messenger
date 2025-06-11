package org.example.project.richtext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.HorizontalScrollbar
import net.folivo.trixnity.core.model.Mention
import org.example.project.em
import org.example.project.html.ListScope

data class RichTextContext(
    val listScope: ListScope? = null,
    val preformatted: Boolean = false,
    val textLinkStyles: TextLinkStyles? = null,
    val spoilerStyles: TextLinkStyles? = null,
    var mentions: MutableSet<String> = mutableSetOf(),
    val onCopy: (String) -> Unit = {},
    val onLinkClick: (String) -> Unit = {},
    val onMentionClick: (Mention) -> Unit = {},
)

@Composable
internal fun ColumnScope.Children(node: RichText.Block, context: RichTextContext) {
    for (index in node.children.indices) {
        val child = node.children[index]
        when (child) {
            is RichText.Block -> BlockContent(child, context)
            is RichText.InlineSpan -> InlineContent(child, context, first = index == 0)
        }
    }
}

@Composable
fun ColumnScope.BlockContent(node: RichText.Block, context: RichTextContext) {
    when (node.tag) {
        "mx-reply", "head" -> Unit

        "h1" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.headlineLarge,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "h2" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.headlineMedium,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "h3" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.headlineSmall,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "h4" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleLarge,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "h5" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleMedium,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "h6" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleSmall,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "blockquote" -> {
            val borderColor = MaterialTheme.colorScheme.outlineVariant
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    Modifier.drawBehind {
                        drawRect(borderColor, size = size.copy(width = 4.dp.toPx()), topLeft = Offset(0f, 0f))
                    }.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)
                ) {
                    Children(node, context)
                }
            }
        }

        "#root", "html", "body", "div", "p" -> {
            Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                Children(node, context)
            }
        }

        "ul" -> {
            Column {
                Children(
                    node, context.copy(
                        listScope = (context.listScope as? ListScope.UnorderedList)?.run {
                            copy(
                                type = ListScope.UnorderedListStyle.of(node.attributes["type"])
                                    ?: ListScope.UnorderedListStyle.next(type)
                            )
                        } ?: ListScope.UnorderedList(
                            type = ListScope.UnorderedListStyle.of(node.attributes["type"])
                                ?: ListScope.UnorderedListStyle.CIRCLE
                        ),
                    ))
            }
        }

        "ol" -> {
            Column {
                Children(
                    node, context.copy(
                        listScope = (context.listScope as? ListScope.OrderedList)?.run {
                            copy(
                                index = node.attributes["start"]?.toIntOrNull() ?: 1,
                                type = ListScope.OrderedListStyle.of(node.attributes["type"]) ?: type,
                                reversed = node.attributes["reversed"]?.toBooleanStrictOrNull() ?: false
                            )
                        } ?: ListScope.OrderedList(
                            index = node.attributes["start"]?.toIntOrNull() ?: 1,
                            type = ListScope.OrderedListStyle.of(node.attributes["type"])
                                ?: ListScope.OrderedListStyle.NUMBERS,
                            reversed = node.attributes["reversed"]?.toBooleanStrictOrNull() ?: false
                        ),
                    ))
            }
        }

        "li" -> {
            Row {
                val listIcon = remember {
                    context.listScope?.render()
                        ?: ListScope.UnorderedListStyle.CIRCLE.symbol
                }

                DisableSelection {
                    Text(
                        listIcon,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier.padding(end = 0.5.em).width(2.em),
                    )
                }
                Column {
                    Children(node, context)
                }
            }
        }

        "hr" -> {
            HorizontalDivider()
        }

        "table" -> {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
            ) {
                val scrollState = rememberScrollState()
                Layout(
                    content = {
                        DataTable(
                            node,
                            context,
                            modifier = Modifier
                                .horizontalScroll(scrollState)
                                .padding(8.dp)
                        )
                        HorizontalScrollbar(Modifier, scrollState)
                    },
                    measurePolicy = HorizontalScrollableMeasurePolicy
                )
            }
        }

        "caption" -> {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleMedium,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.em)) {
                    Children(node, context)
                }
            }
        }

        "pre" -> {
            val highlightedCode = rememberHighlightedCode(node)

            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                Column {
                    highlightedCode?.let {
                        Surface(
                            tonalElevation = 16.dp,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.height(48.dp).padding(start = 16.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(highlightedCode.language)
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        context.onCopy(highlightedCode.content.text)
                                    },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                    val scrollState = rememberScrollState()
                    Layout(
                        content = {
                            Column(
                                Modifier
                                    .horizontalScroll(scrollState)
                                    .padding(8.dp)
                            ) {
                                highlightedCode?.content?.let {
                                    Text(
                                        it,
                                        fontFamily = FontFamily.Monospace,
                                        softWrap = false,
                                    )
                                } ?: Children(
                                    node,
                                    context.copy(preformatted = true)
                                )
                            }
                            HorizontalScrollbar(Modifier, scrollState)
                        },
                        measurePolicy = HorizontalScrollableMeasurePolicy
                    )
                }
            }
        }

        "details" -> {
            val summary = node.children.filterIsInstance<RichText.Block>().firstOrNull { it.tag == "summary" }
            val children = node.children.filter { it != summary }

            val expanded = remember { mutableStateOf(false) }

            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                Column {
                    Surface(
                        tonalElevation = 16.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.height(48.dp).padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                if (summary != null) {
                                    BlockContent(summary, context)
                                } else {
                                    Text("Details without summary.")
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { expanded.value = !expanded.value },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Icon(
                                    if (expanded.value) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    AnimatedVisibility(expanded.value) {
                        Column(Modifier.padding(8.dp)) {
                            for (index in children.indices) {
                                val child = children[index]
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

        "summary" -> {
            Children(node, context)
        }

        "img" -> {
            // TODO
        }

        else -> Unit
    }
}

