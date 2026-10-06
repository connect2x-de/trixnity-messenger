package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.richtext.html.parseColor
import de.connect2x.trixnity.messenger.util.isMatrixMediaUri

@Composable
internal fun InlineContent(content: RichText.InlineSpan, context: RichTextContext, first: Boolean) {
    val i18n = DI.current.get<I18nView>()

    val inlineContext = rememberInlineRichTextContext(context)
    val measurer = rememberMentionChipMeasurer(inlineContext, i18n)

    val rendered =
        remember(content.children, inlineContext, context.loadImage != null) {
            renderInlineContent(content, inlineContext, first, context.loadImage != null)
        }
    val images = rendered.images
    val maxImageHeight = LocalInlineImageMaxHeight.current?.value
    val bitmaps =
        images.values
            .mapNotNull { it.attributes["src"] }
            .toSet()
            .associateWith { uri -> key(uri) { rememberInlineImage(uri, context.loadImage) } }
    val imageMeasurer: CompoundTextContext.(String, Constraints) -> Placeholder? =
        remember(images, bitmaps, measurer, maxImageHeight) {
            { key, constraints ->
                images[key]?.let { measureInlineImage(it, bitmaps[it.attributes["src"]], constraints, maxImageHeight) }
                    ?: measurer(key, constraints)
            }
        }

    CompoundText(content = rendered.text, onMeasure = imageMeasurer) { url, constraints ->
        val mention = inlineContext.mentions?.get(url)
        val image = images[url]
        if (image != null) {
            bitmaps[image.attributes["src"]]?.let { InlineImage(image, it) }
        } else if (mention != null) {
            MentionChip(mention, i18n, constraints, onMentionClick = context.onMentionClick)
        }
    }
}

internal data class RenderedInlineContent(val text: AnnotatedString, val images: Map<String, RichText.Inline.Block>)

internal fun renderInlineContent(
    content: RichText.InlineSpan,
    context: InlineRichTextContext,
    first: Boolean,
    allowImages: Boolean = true,
): RenderedInlineContent {
    val images = mutableMapOf<String, RichText.Inline.Block>()
    val imageContext = context.copy(onImage = { key, image -> images[key] = image }, allowImages = allowImages)
    val text = buildAnnotatedString { Children(content, imageContext, first) }
    return RenderedInlineContent(text, images.toMap())
}

internal fun AnnotatedString.Builder.Children(
    node: RichText.Inline.Block,
    context: InlineRichTextContext,
    first: Boolean,
) {
    for (index in node.children.indices) {
        when (val child = node.children[index]) {
            is RichText.Inline.Block -> inlineContent(child, context, first = first && index == 0)
            is RichText.Inline.Text -> inlineContent(child, context, first = first && index == 0)
        }
    }
}

internal fun AnnotatedString.Builder.Children(
    node: RichText.InlineSpan,
    context: InlineRichTextContext,
    first: Boolean,
) {
    for (index in node.children.indices) {
        when (val child = node.children[index]) {
            is RichText.Inline.Block -> inlineContent(child, context, first = first && index == 0)
            is RichText.Inline.Text -> inlineContent(child, context, first = first && index == 0)
        }
    }
}

internal fun AnnotatedString.Builder.inlineContent(
    node: RichText.Inline.Text,
    context: InlineRichTextContext,
    first: Boolean,
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
    first: Boolean,
) {
    if (context.preformatted && node.rawContent != null) {
        append(node.rawContent)
    } else {
        when (node.tag) {
            "del",
            "s" -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                Children(node, context, first)
                pop()
            }

            "a" -> {
                val href = node.attributes["href"] ?: ""
                if (context.mentions?.get(href) != null) {
                    pushStringAnnotation(INLINE_CONTENT_TAG, href)
                    Children(node, context.copy(allowImages = false), first)
                    pop()
                } else if (
                    href.startsWith("https:") ||
                        href.startsWith("http:") ||
                        href.startsWith("ftp:") ||
                        href.startsWith("mailto:") ||
                        href.startsWith("magnet:")
                ) {
                    pushLink(
                        LinkAnnotation.Url(
                            url = href,
                            styles = context.textLinkStyles,
                            linkInteractionListener = { context.onLinkClick(href) },
                        )
                    )
                    Children(node, context, first)
                    pop()
                } else {
                    Children(node, context, first)
                }
            }

            "sup" -> {
                pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript))
                Children(node, context, first)
                pop()
            }

            "sub" -> {
                pushStyle(SpanStyle(baselineShift = BaselineShift.Subscript))
                Children(node, context, first)
                pop()
            }

            "b",
            "strong" -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                Children(node, context, first)
                pop()
            }

            "i",
            "em" -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                Children(node, context, first)
                pop()
            }

            "u" -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                Children(node, context, first)
                pop()
            }

            "code" -> {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
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
                    Children(node, context.copy(allowImages = false), first)
                    pop()
                }
                    ?: run {
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
                val alt = node.attributes["alt"] ?: node.attributes["title"] ?: "\uFFFC"
                if (context.allowImages && isMatrixMediaUri(node.attributes["src"])) {
                    val key = "image:$length"
                    context.onImage?.invoke(key, node)
                    pushStringAnnotation(INLINE_CONTENT_TAG, key)
                    append(alt.ifBlank { "\uFFFC" })
                    pop()
                } else {
                    append(alt)
                }
            }
        }
    }
}
