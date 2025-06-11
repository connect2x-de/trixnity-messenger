package org.example.project.richtext

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import net.folivo.trixnity.core.MatrixRegex
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.events.m.Mentions
import org.example.project.html.parseColor

@Composable
internal fun InlineContent(content: RichText.InlineSpan, context: RichTextContext, first: Boolean) {
    val content = remember(content.children) {
        val annotatedContent = buildAnnotatedString {
            Children(content, context, first)
        }
        autoLinkify(annotatedContent, context)
    }
    val chipHeight = 28.dp
    val chipPadding = 56.dp
    val chipStyle = MaterialTheme.typography.labelLarge

    CompoundText(
        content = content,
        onMeasure = { url ->
            val mention = MatrixRegex.findMentions(url).values.single()
            val measurement = measure(mention.label ?: mention.match, chipStyle)
            Placeholder(
                width = with(density) { (measurement.size.width.toDp() + chipPadding).toSp() },
                height = with(density) { chipHeight.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
        }
    ) { url ->
        val density = LocalDensity.current
        val mention = MatrixRegex.findMentions(url).values.single()
        DisableSelection {
            ElevatedSuggestionChip(
                label = { Text(mention.label ?: mention.match) },
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                onClick = { context.onMentionClick(mention) },
                modifier = with(density) {
                    Modifier.pointerHoverIcon(PointerIcon.Hand)
                }
            )
        }
    }
}

internal fun AnnotatedString.Builder.Children(node: RichText.Inline.Block, context: RichTextContext, first: Boolean) {
    for (index in node.children.indices) {
        val child = node.children[index]
        when (child) {
            is RichText.Inline.Block -> inlineContent(child, context, first = first && index == 0)
            is RichText.Inline.Text -> inlineContent(child, context, first = first && index == 0)
        }
    }
}

internal fun AnnotatedString.Builder.Children(node: RichText.InlineSpan, context: RichTextContext, first: Boolean) {
    for (index in node.children.indices) {
        val child = node.children[index]
        when (child) {
            is RichText.Inline.Block -> inlineContent(child, context, first = first && index == 0)
            is RichText.Inline.Text -> inlineContent(child, context, first = first && index == 0)
        }
    }
}

internal fun AnnotatedString.Builder.inlineContent(
    node: RichText.Inline.Text,
    context: RichTextContext,
    first: Boolean
) {
    val content = if (context.preformatted) node.fullContent else node.content
    append(if (first) content.trimStart() else content)
}

internal fun AnnotatedString.Builder.inlineContent(
    node: RichText.Inline.Block,
    context: RichTextContext,
    first: Boolean
) {
    when (node.tag) {
        "del", "s" -> {
            pushStyle(SpanStyle(textDecoration = TextDecoration.Companion.LineThrough))
            Children(node, context, first)
            pop()
        }

        "a" -> {
            val href = node.attributes["href"] ?: ""
            val mention = MatrixRegex.findMentions(href).values.singleOrNull()
            if (mention != null) {
                context.mentions.add(href)

                pushStringAnnotation(INLINE_CONTENT_TAG, href)
                Children(node, context, first)
                pop()
            } else if (href.startsWith("https:") || href.startsWith("http:")
                || href.startsWith("ftp:") || href.startsWith("mailto:")
                || href.startsWith("magnet:")
            ) {
                pushLink(
                    LinkAnnotation.Url(
                        url = href,
                        styles = context.textLinkStyles,
                        linkInteractionListener = {
                            context.onLinkClick(href)
                        }
                    )
                )
                Children(node, context, first)
                pop()
            } else {
                Children(node, context, first)
            }
        }

        "sup" -> {
            pushStyle(SpanStyle(baselineShift = BaselineShift.Companion.Superscript))
            Children(node, context, first)
            pop()
        }

        "sub" -> {
            pushStyle(SpanStyle(baselineShift = BaselineShift.Companion.Subscript))
            Children(node, context, first)
            pop()
        }

        "b", "strong" -> {
            pushStyle(SpanStyle(fontWeight = FontWeight.Companion.Bold))
            Children(node, context, first)
            pop()
        }

        "i", "em" -> {
            pushStyle(SpanStyle(fontStyle = FontStyle.Companion.Italic))
            Children(node, context, first)
            pop()
        }

        "u" -> {
            pushStyle(SpanStyle(textDecoration = TextDecoration.Companion.Underline))
            Children(node, context, first)
            pop()
        }

        "code" -> {
            pushStyle(
                SpanStyle(
                    fontFamily = FontFamily.Companion.Monospace,
                )
            )
            Children(node, context, first)
            pop()
        }

        "br" -> {
            appendLine()
        }

        "span" -> {
            node.attributes["data-mx-spoiler"]?.let { reason ->
                pushLink(
                    LinkAnnotation.Clickable(
                        tag = "spoiler",
                        styles = context.spoilerStyles,
                        linkInteractionListener = null,
                    )
                )
                pushStyle(
                    SpanStyle(
                        background = parseColor(node.attributes["data-mx-bg-color"] ?: ""),
                        color = parseColor(node.attributes["data-mx-color"] ?: ""),
                    )
                )
                Children(node, context, first)
                pop()
            } ?: run {
                pushStyle(
                    SpanStyle(
                        background = parseColor(node.attributes["data-mx-bg-color"] ?: ""),
                        color = parseColor(node.attributes["data-mx-color"] ?: ""),
                    )
                )
                Children(node, context, first)
                pop()
            }
        }

        "img" -> {
            // TODO
        }
    }
}
