package org.example.project.richtext

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import org.example.project.html.Patterns

internal fun autoLinkify(content: AnnotatedString, context: RichTextContext): AnnotatedString =
    buildAnnotatedString {
        append(content)
        for (it in Patterns.WEB_URL.findAll(content)) {
            if (content.getLinkAnnotations(it.range.first, it.range.last + 1).isEmpty()) {
                // Parenthesis handling
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