package de.connect2x.messenger.compose.view.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.richtext.html.parseColor

@Composable
internal fun InlineContent(
    content: RichText.InlineSpan,
    context: RichTextContext,
    first: Boolean,
) {
    val i18n = DI.current.get<I18nView>()

    val inlineContext = rememberInlineRichTextContext(context)
    val measurer = rememberMentionChipMeasurer(inlineContext, i18n)

    val content = remember(content.children, inlineContext) {
        buildAnnotatedString {
            Children(content, inlineContext, first)
        }
    }

    CompoundText(
        content = content,
        onMeasure = measurer,
    ) { url, constraints ->
        val mention = inlineContext.mentions?.get(url)
        if (mention != null) {
            MentionChip(mention, i18n, constraints, onMentionClick = context.onMentionClick)
        }
    }
}

internal fun AnnotatedString.Builder.Children(node: RichText.Inline.Block, context: InlineRichTextContext, first: Boolean) {
    for (index in node.children.indices) {
        val child = node.children[index]
        when (child) {
            is RichText.Inline.Block -> inlineContent(child, context, first = first && index == 0)
            is RichText.Inline.Text -> inlineContent(child, context, first = first && index == 0)
        }
    }
}

internal fun AnnotatedString.Builder.Children(node: RichText.InlineSpan, context: InlineRichTextContext, first: Boolean) {
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
    context: InlineRichTextContext,
    first: Boolean
) {
    if (context.preformatted && node.rawContent != null) {
        append(node.rawContent)
    } else {
        append(if (first) node.content.trimStart() else node.content)
    }
}

internal fun AnnotatedString.Builder.inlineContent(
    node: RichText.Inline.Block,
    context: InlineRichTextContext,
    first: Boolean
) {
    if (context.preformatted && node.rawContent != null) {
        append(node.rawContent)
    } else {
        when (node.tag) {
            "del", "s" -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.Companion.LineThrough))
                Children(node, context, first)
                pop()
            }

            "a" -> {
                val href = node.attributes["href"] ?: ""
                if (context.mentions?.get(href) != null) {
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
}
