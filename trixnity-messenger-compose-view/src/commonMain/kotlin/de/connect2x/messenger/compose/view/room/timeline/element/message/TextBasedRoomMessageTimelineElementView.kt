package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Platform
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.isDesktop
import de.connect2x.messenger.compose.view.room.timeline.element.util.formatMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.mentionsUriHandler
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel

@Composable
fun TextBasedRoomMessageTimelineElementView(
    holder: BaseTimelineElementHolderViewModel,
    element: RoomMessageTimelineElementViewModel.TextBased.Text, // FIXME RoomMessageTimelineElementViewModel.TextBased<*>,
) {
    MessageBubble(
        holder,
        element,
        showDate = true,
        needsMaxWidth = true, // FIXME ?
        // FIXME context menu actions
    ) {
        MessageText(holder, element)
    }
}

@Composable
private fun MessageText(
    holder: BaseTimelineElementHolderViewModel,
    textBasedRoomMessageTimelineElementViewModel: RoomMessageTimelineElementViewModel.TextBased.Text,
) {
    if (Platform.current.isDesktop) {
        // on Desktop it makes sense to select text and copy it;
        // on Android, this will consume long tap events, which we use for the context menu
        SelectionContainer {
            MessageTextContent(holder, textBasedRoomMessageTimelineElementViewModel)
        }
    } else {
        MessageTextContent(holder, textBasedRoomMessageTimelineElementViewModel)
    }
}


@Composable
private fun MessageTextContent(
    holder: BaseTimelineElementHolderViewModel,
    textBasedRoomMessageTimelineElementViewModel: RoomMessageTimelineElementViewModel.TextBased.Text,
) {
//    val referencedMessage = holder.repliedElement.collectAsState().value // FIXME in parent element?

    val i18n = DI.get<I18nView>()

    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)) {
        // FIXME own View?
//        if (textBasedRoomMessageTimelineElementViewModel is NoticeMessageViewModel) {
//            Row {
//                Icon(Icons.Filled.SmartToy, i18n.automated())
//                Text(i18n.automated())
//            }
//
//            Spacer(Modifier.size(5.dp))
//        }

//        if (referencedMessage != null) {
//            val sender = referencedMessage.sender
//            ReferencedMessagePill(
//                senderName = sender.name,
//                senderNameColor = MaterialTheme.messengerColors.getUserColor(sender.userId),
//                content = {
//                    when (referencedMessage) {
//                        is ReferencedMessage.ReferencedTextMessage -> TextReply(referencedMessage.messageShortened())
//                        is ReferencedMessage.ReferencedImageMessage ->
//                            referencedMessage.thumbnail?.let { imageBitmapFromBytes(it) }
//                                ?.let { imageBitmap ->
//                                    ImageReply(imageBitmap)
//                                } ?: ImageReplyDefault(referencedMessage.fileName)
//
//                        is ReferencedMessage.ReferencedVideoMessage ->
//                            referencedMessage.thumbnail?.let { imageBitmapFromBytes(it) }
//                                ?.let { imageBitmap ->
//                                    VideoReply(imageBitmap)
//                                } ?: VideoReplyDefault(referencedMessage.fileName)
//
//                        is ReferencedMessage.ReferencedAudioMessage -> AudioReply(referencedMessage.fileName)
//                        is ReferencedMessage.ReferencedFileMessage -> FileReply(referencedMessage.fileName)
//                        is ReferencedMessage.ReferencedLocationMessage -> LocationReply(
//                            referencedMessage.name,
//                            referencedMessage.geoUri,
//                        )
//
//                        is ReferencedMessage.ReferencedUnknownMessage -> UnknownReply()
//                    }
//                },
//            )
//            Spacer(Modifier.size(5.dp))
//        }

        val mentions = (textBasedRoomMessageTimelineElementViewModel.mentionsInFormattedBody
            ?: textBasedRoomMessageTimelineElementViewModel.mentionsInBody)
            .map {
                it.key to it.value.collectAsState().value
            }.sortedByDescending { it.first.first }

        val message = textBasedRoomMessageTimelineElementViewModel.formattedBody
            ?: textBasedRoomMessageTimelineElementViewModel.body
        val text = formatMessage(message, mentions, textBasedRoomMessageTimelineElementViewModel)

        val richTextState = rememberRichTextState()
        LaunchedEffect(text) {
            richTextState.setHtml(text)
        }
        richTextState.config.linkColor =
            if (holder.isByMe) MaterialTheme.messengerColors.linkByMe // Inherit link color from Messenger colors
            else MaterialTheme.messengerColors.link

        if (richTextState.toHtml().isNotBlank()) {
            if (mentions.any { it.second != null }) {
                val baseUriHandler = LocalUriHandler.current
                val uriHandler by remember {
                    mentionsUriHandler(
                        baseUriHandler,
                        textBasedRoomMessageTimelineElementViewModel,
                        mentions.map { it.second })
                }

                MessageRichText(
                    uriHandler,
                    richTextState,
                    holder.isByMe,
                    onLongPress
                )
            } else {
                MessageRichText(
                    LocalUriHandler.current,
                    richTextState,
                    holder.isByMe,
                    onLongPress
                )
            }
        } else {
            // workaround for 1st rendering cycle where nothing is displayed since the RichText's HTML is set in an effect
            Text(textBasedRoomMessageTimelineElementViewModel.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageRichText(
    uriHandler: UriHandler,
    state: RichTextState,
    isByMe: Boolean,
    onLongPress: (Offset) -> Unit
) {
    CompositionLocalProvider(
        LocalUriHandler provides uriHandler
    ) {
        BasicRichText(
            state = state,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = onLongPress
                )
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isByMe) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondary
            )
        )
    }
}
