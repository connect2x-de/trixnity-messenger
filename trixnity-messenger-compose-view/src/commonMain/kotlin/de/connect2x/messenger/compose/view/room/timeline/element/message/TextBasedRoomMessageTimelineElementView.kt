package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isDesktop
import de.connect2x.messenger.compose.view.isWeb
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.messenger.compose.view.room.timeline.element.util.mentionsUriHandler
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementMention
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import io.ktor.http.Url

@Composable
fun TextBasedRoomMessageTimelineElementView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
    isPreview: Boolean,
) {
    MessageBubble(
        holder,
        needsMaxWidth = false,
        isPreview = isPreview
    ) { showActionMenu ->
        // on Desktop and Web, it makes sense to select text and copy it;
        // on Android, this will consume long tap events, which we use for the context menu
        when (Platform.current) {
            PlatformType.DESKTOP, PlatformType.WEB -> SelectionContainer {
                MessageTextContent(holder, element, showActionMenu)
            }
            PlatformType.ANDROID -> MessageTextContent(holder, element, showActionMenu)
        }
    }
}

@Composable
private fun MessageTextContent(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
    showActionMenu: () -> Unit,
) {
    val i18n = DI.get<I18nView>()

    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
        if (element is RoomMessageTimelineElementViewModel.TextBased.Notice) {
            Row {
                Icon(Icons.Filled.SmartToy, i18n.automated())
                Text(i18n.automated(), fontStyle = FontStyle.Italic)
            }

            Spacer(Modifier.size(5.dp))
        }

        if (element is RoomMessageTimelineElementViewModel.TextBased.Emote) {
            Text("${holder.sender.collectAsState().value?.name}", fontStyle = FontStyle.Italic)
            Spacer(Modifier.size(5.dp))
        }

        val mentions = (element.mentionsInFormattedBody
            ?: element.mentionsInBody)
            .map {
                it.key to it.value.collectAsState().value
            }.sortedByDescending { it.first.first }

        val message = element.formattedBody ?: element.body
        val text = formatMessage(message, mentions)

        val richTextState = rememberSaveable(text, saver = RichTextState.Saver) {
            RichTextState().apply {
                setHtml(text)
            }
        }
        richTextState.config.linkColor =
            if (holder.isByMe) MaterialTheme.messengerColors.linkByMe // Inherit link color from Messenger colors
            else MaterialTheme.messengerColors.link

        if (mentions.any { it.second != null }) {
            val baseUriHandler = LocalUriHandler.current
            val uriHandler by remember {
                mentionsUriHandler(
                    baseUriHandler,
                    element,
                    mentions.map { it.second })
            }

            MessageRichText(
                uriHandler,
                richTextState,
                showActionMenu,
            )
        } else {
            MessageRichText(
                LocalUriHandler.current,
                richTextState,
                showActionMenu,
            )
        }
    }
}

@Composable
private fun MessageRichText(
    uriHandler: UriHandler,
    state: RichTextState,
    showActionMenu: () -> Unit
) {
    CompositionLocalProvider(
        LocalUriHandler provides uriHandler
    ) {
        BasicRichText(
            state = state,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showActionMenu() }
                )
            },
            style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current)
        )
    }
}

@Composable
private fun formatMessage(
    message: String,
    mentions: List<Pair<IntRange, TimelineElementMention?>>,
): String {
    val i18n = DI.get<I18nView>()
    return remember(message, mentions) {
        message
            .formatMentions(mentions, i18n::eventMentionPile)
            .formatLinks()
            .replace("\n", "<br>")
    }
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


// For normal text we could only use <space> as ending delimiter,
// however as our text might include html `<a href="..."></a>` which
// ends with a quote we need to use that as well.
// A better way would be to use a proper html parser and then
// only run the regex on normal text element with only the space.
//
// Instead of having a complicated url regex which lead to multiple problems
// https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger/-/issues/440
// https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger/-/issues/350
// https://gitlab.com/connect2x/trixnity-messenger/trixnity-messenger/-/issues/296
// we use the most basic regex to find potential links and then use ktor's url parser
// to validate if it really was a proper Url.
private val maybeUrlRegex =
    Regex("""https?://[^ "<]*""")

private val commonPunctuation =
    setOf('.', '!', '?', ':', ')')

private val hrefPrefix =
    "href=\""

private fun String.isValidUrl() =
    runCatching { Url(this) }.isSuccess

internal fun String.formatLinks(): String {
    fun MatchResult.isInsideHref(): Boolean {
        val fullString = this@formatLinks

        return range.first > hrefPrefix.length
            && range.last != fullString.length
            && fullString.substring(range.first - hrefPrefix.length, range.first) == hrefPrefix
            && fullString[range.last + 1] == '"'
    }

    fun MatchResult.formatLinkAsHref(): String {
        val end = if (value.last() in commonPunctuation) value.length - 1
                  else value.length

        val withoutPunct = value.substring(0..<end)
        val punct = value.substring(end..<value.length)

        return "<a href=\"$withoutPunct\">$withoutPunct</a>$punct"
    }

    return replace(maybeUrlRegex) {
        if (it.isInsideHref() || !it.value.isValidUrl()) {
            it.value
        } else {
            it.formatLinkAsHref()
        }
    }
}

@Composable
fun TextReplyInTimeline(
    holder: TimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
) {
    ReferencedMessagePill(
        holder = holder,
        content = {
            TextReply(element, 4)
        }
    )
}

@Composable
fun TextReplyInSendMessage(
    holder: TimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>
) {
    ReferencedMessagePill(
        holder = holder,
        content = {
            TextReply(element, 2)
        }
    )
}

@Composable
private fun TextReply(element: RoomMessageTimelineElementViewModel.TextBased<*>, maxLines: Int) {
    Text(
        text = element.body,
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
