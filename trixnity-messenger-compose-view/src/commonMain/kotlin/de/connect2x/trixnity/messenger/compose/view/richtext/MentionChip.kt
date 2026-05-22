package de.connect2x.trixnity.messenger.compose.view.richtext

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ChipStyle
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSuggestionChip
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention

internal fun mentionLabel(i18n: I18nView, mention: TimelineElementMention): String =
    when (mention) {
        is TimelineElementMention.Event -> i18n.mentionEventInRoom(mention.room.name)
        is TimelineElementMention.Room -> mention.room.name
        is TimelineElementMention.User -> mention.user.name
    }

private val iconModifier = Modifier.requiredSize(SuggestionChipDefaults.IconSize)

@Composable
internal fun MentionChip(
    mention: TimelineElementMention,
    i18n: I18nView,
    constraints: Constraints,
    style: ChipStyle = MaterialTheme.components.mentionChip,
    onMentionClick: (TimelineElementMention) -> Unit,
) {
    val density = LocalDensity.current
    val maxWidth = with(density) { constraints.maxWidth.toDp() }
    DisableSelection {
        ThemedSuggestionChip(
            style = style,
            label = { Text(mentionLabel(i18n, mention), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            icon = {
                when (mention) {
                    is TimelineElementMention.Event ->
                        Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = iconModifier)

                    is TimelineElementMention.Room ->
                        Icon(Icons.Default.Numbers, contentDescription = null, modifier = iconModifier)

                    is TimelineElementMention.User ->
                        Icon(Icons.Default.Person, contentDescription = null, modifier = iconModifier)
                }
            },
            onClick = { onMentionClick(mention) },
            modifier = Modifier.requiredWidthIn(max = maxWidth).pointerHoverIcon(PointerIcon.Hand).padding(all = 1.dp),
        )
    }
}

/**
 * It is ABSOLUTELY NECESSARY that this function always returns the same size as the actual MentionChip That's why
 * mentionLabel was refactored out, and why the icons are a fixed size
 */
@Composable
internal fun rememberMentionChipMeasurer(
    context: InlineRichTextContext,
    i18n: I18nView,
): (CompoundTextContext.(String, Constraints) -> Placeholder?) {
    val textStyle = MaterialTheme.typography.labelLarge
    return remember(context, i18n, textStyle) {
        val chipMinHeight = SuggestionChipDefaults.Height
        val leadingIconSize = SuggestionChipDefaults.IconSize
        val horizontalElementsPadding = 8.dp
        val margin = 1.dp

        { url, constraints ->
            context.mentions
                ?.get(url)
                ?.let { measure(mentionLabel(i18n, it), textStyle) }
                ?.let {
                    val maxWidth = with(density) { constraints.maxWidth.toDp() }
                    val horizontalPadding = (horizontalElementsPadding * 4) + leadingIconSize + (margin * 2)
                    val labelPlaceableWidth =
                        with(density) { it.size.width.toDp() }.coerceAtMost(maxWidth - horizontalPadding)
                    val labelPlaceableHeight = with(density) { it.size.height.toDp() }

                    val width = (horizontalElementsPadding * 4) + leadingIconSize + labelPlaceableWidth + (margin * 2)
                    val height = maxOf(chipMinHeight, leadingIconSize, labelPlaceableHeight) + (margin * 2)

                    Placeholder(
                        width = with(density) { width.toSp() },
                        height = with(density) { height.toSp() },
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    )
                }
        }
    }
}
