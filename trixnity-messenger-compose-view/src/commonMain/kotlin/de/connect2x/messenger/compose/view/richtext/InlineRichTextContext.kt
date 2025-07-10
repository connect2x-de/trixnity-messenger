package de.connect2x.messenger.compose.view.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextLinkStyles
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention

data class InlineRichTextContext(
    val preformatted: Boolean = false,
    val textLinkStyles: TextLinkStyles? = null,
    val spoilerStyles: TextLinkStyles? = null,
    val mentions: Map<String, TimelineElementMention?>?,
    val onCopy: (String) -> Unit = {},
    val onLinkClick: (String) -> Unit = {},
    val onMentionClick: (TimelineElementMention) -> Unit = {},
)

@Composable
internal fun rememberInlineRichTextContext(context: RichTextContext): InlineRichTextContext {
    val mentions = context.mentions?.collectAsState()?.value
    return remember(context, mentions) {
        InlineRichTextContext(
            context.preformatted,
            context.textLinkStyles,
            context.spoilerStyles,
            mentions,
            context.onCopy,
            context.onLinkClick,
            context.onMentionClick,
        )
    }
}
