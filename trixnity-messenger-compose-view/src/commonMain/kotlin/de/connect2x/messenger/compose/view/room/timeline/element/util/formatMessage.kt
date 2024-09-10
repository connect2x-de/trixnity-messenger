package de.connect2x.messenger.compose.view.room.timeline.element.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.urlRegex
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EmoteMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TextBasedViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util.MessageMention

@Composable
fun formatMessage(message: String, mentions: List<Pair<IntRange, MessageMention?>>, viewmodel: TextBasedViewModel): String {
    val i18n = DI.current.get<I18nView>()
    return message
        .formatMentions(mentions, i18n::eventMentionPile)
        .handleBodyTypes(viewmodel.formattedBody != null)
        .formatLinks()
        .replace("\n", "<br>")
        .handleViewmodelTypes(viewmodel)
}

internal fun String.formatMentions(
    mentions: List<Pair<IntRange, MessageMention?>>,
    eventPile: (String) -> String
): String =
    mentions.foldIndexed(this) { index, currentText, (range, mention) ->
        val anchorContent = when (mention) {
            is MessageMention.Event -> eventPile(mention.room.name)
            is MessageMention.Room -> mention.room.name
            is MessageMention.User -> mention.user.name

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
internal fun String.handleViewmodelTypes(viewmodel: TextBasedViewModel): String =
    if (viewmodel is EmoteMessageViewModel) {
        "${viewmodel.sender.collectAsState().value.name} $this"
    } else this
