package de.connect2x.messenger.compose.view.richtext

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import de.connect2x.messenger.compose.view.richtext.html.Patterns

internal fun autoLinkify(content: AnnotatedString, context: RichTextContext): AnnotatedString =
    buildAnnotatedString {
        append(content)
        for (it in Patterns.WEB_URL.findAll(content)) {
            if (content.getLinkAnnotations(it.range.first, it.range.last + 1).isEmpty()) {
                val href = it.value
                addLink(
                    LinkAnnotation.Url(
                        url = href,
                        styles = context.textLinkStyles,
                        linkInteractionListener = {
                            context.onLinkClick(href)
                        }
                    ),
                    it.range.start,
                    it.range.last + 1,
                )
            }
        }
    }
