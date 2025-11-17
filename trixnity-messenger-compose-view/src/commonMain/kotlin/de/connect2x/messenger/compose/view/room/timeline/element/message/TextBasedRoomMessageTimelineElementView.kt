package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.PlatformType
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.richtext.RichTextColors
import de.connect2x.messenger.compose.view.richtext.RichTextDisplay
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubble
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.ReferencedMessagePill
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedSelectionContainer
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.messenger.compose.view.util.rovingFocusChild
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun TextBasedRoomMessageTimelineElementView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
    isPreview: Boolean,
    index: Int,
) {
    MessageBubble(
        holder,
        needsMaxWidth = false,
        isPreview = isPreview,
        index = index,
    ) { showActionMenu ->
        TextRoomMessageTimelineElementView(holder, element, showActionMenu)
    }
}

@Composable
fun TextRoomMessageTimelineElementView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel<*>,
    showActionMenu: () -> Unit,
) {
    // on Desktop and Web, it makes sense to select text and copy it;
    // on Android and iOS, this will consume long tap events, which we use for the context menu
    when (Platform.current) {
        PlatformType.DESKTOP, PlatformType.WEB -> ThemedSelectionContainer(
            modifier = Modifier.rovingFocusChild(),
            style = if (holder.isByMe) MaterialTheme.components.selectionOnPrimary else MaterialTheme.components.selectionOnSurface
        ) {
            MessageTextContent(holder, element, showActionMenu)
        }

        PlatformType.ANDROID, PlatformType.IOS -> MessageTextContent(holder, element, showActionMenu)
    }
}

@Composable
private fun MessageTextContent(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel<*>,
    showActionMenu: () -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val uriCaller = DI.get<UriCaller>()
    val content = element.formattedBodyContent
    val sender = holder.sender.collectAsState().value

    Column(
        Modifier
            .padding(start = 10.dp, end = 10.dp, top = 10.dp)
    ) {
        if (element is RoomMessageTimelineElementViewModel.TextBased.Notice) {
            Row {
                Icon(Icons.Filled.SmartToy, i18n.automated())
                Text(i18n.automated(), fontStyle = FontStyle.Italic)
            }

            Spacer(Modifier.size(5.dp))
        }

        if (element is RoomMessageTimelineElementViewModel.TextBased.Emote) {
            Text("${sender?.name}", fontStyle = FontStyle.Italic)
            Spacer(Modifier.size(5.dp))
        }


        if (content != null) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current)
            ) {
                RichTextDisplay(
                    document = content,
                    mentions = element.mentionsInFormattedBody,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { showActionMenu() }
                            )
                        },
                    colors = RichTextColors.default(
                        linkColor =
                            if (holder.isByMe) MaterialTheme.messengerColors.linkByMe // Inherit link color from Messenger colors
                            else MaterialTheme.messengerColors.link
                    ),
                    onCopy = null,
                    onLinkClick = { uriCaller.invoke(it, true) },
                    onMentionClick = element::openMention
                )
            }
        }
    }
}

@Composable
fun TextReplyInTimeline(
    holder: TimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            TextReply(element, 4)
        }
    )
}

@Composable
fun TextReplyInSendMessage(
    holder: TimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased<*>,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
) {
    ReferencedMessagePill(
        holder = holder,
        modifier = modifier,
        interactionSource = interactionSource,
        content = {
            TextReply(element, 2)
        }
    )
}

@Composable
fun TextReply(element: RoomMessageTimelineElementViewModel<*>, maxLines: Int) {
    Text(
        text = element.body,
        fontStyle = FontStyle.Italic,
        style = MaterialTheme.typography.bodySmall,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
