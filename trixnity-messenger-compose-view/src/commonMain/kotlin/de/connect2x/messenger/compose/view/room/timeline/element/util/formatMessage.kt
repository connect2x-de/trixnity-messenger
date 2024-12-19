package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

internal val urlRegex =
    Regex("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=;]*)")

@Composable
fun formatMessage(
    message: String,
    mentions: List<Pair<IntRange, TimelineElementMention?>>,
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
): String {
    val i18n = DI.get<I18nView>()
    return message
        .formatMentions(mentions, i18n::eventMentionPile)
        .handleBodyTypes(element.formattedBody != null)
        .formatLinks()
        .replace("\n", "<br>")
        .handleViewmodelTypes(holder, element)
}

internal fun String.formatMentions(
    mentions: List<Pair<IntRange, TimelineElementMention?>>,
    eventPile: (String) -> String
): String =
    mentions.foldIndexed(this) { index, currentText, (range, mention) ->
        val anchorContent = when (mention) {
            is TimelineElementMention.Event -> eventPile(mention.room.name)
            is TimelineElementMention.Room -> mention.room.name
            is TimelineElementMention.User -> mention.user.name

            null -> null
        }

        if (anchorContent == null) {
            currentText
        } else {
            currentText.replaceRange(
                range,
                """<a href="timmy-data:$index">$anchorContent</a>"""
            )
        }
    }

internal fun String.handleBodyTypes(isFormattedBody: Boolean): String =
    if (isFormattedBody) {
        this.substringAfter("</mx-reply>").removePrefix("\n")
    } else this

internal fun String.formatLinks(): String =
    this.replace(urlRegex) {
        val href = "href=\""
        if (
            it.range.first > href.length &&
            this.subSequence(it.range.first - href.length, it.range.first) == href &&
            it.range.last != this.length &&
            this[it.range.last + 1] == '"'
        ) {
            it.value
        } else {
            "<a href=\"${it.value}\">${it.value}</a>"
        }
    }

@Composable
internal fun String.handleViewmodelTypes(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>
): String =
    if (element is RoomMessageTimelineElementViewModel.TextBased.Emote) {
        "${holder.sender.collectAsState().value?.name} $this"
    } else this
