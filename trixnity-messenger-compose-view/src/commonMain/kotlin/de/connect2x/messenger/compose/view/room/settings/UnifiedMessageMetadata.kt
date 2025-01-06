package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichText
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.message.formatMessage
import de.connect2x.messenger.compose.view.theme.dp
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


//private val log = KotlinLogging.logger {}

@Composable
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel) {
    val i18n = DI.get<I18nView>()
    val timeZone = DI.get<TimeZone>()
    val edits = viewModel.edits.collectAsState().value
    val reactionCounts = viewModel.reactionCounts.collectAsState().value
    val userInteractions = viewModel.userInteractions.collectAsState().value
    var interactionFilterByReaction by remember { mutableStateOf<ReactionKey?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column {
            Header(viewModel::back, "we need to go back")
            Spacer(Modifier.size(8.dp))
            Text("metadata of ${viewModel.eventId}")
            Spacer(Modifier.size(8.dp))
            Text("Editing history:") // TODO: i18n
            Column {
                edits.sortedBy { it.originTimestamp }.reversed().forEach { item ->
                    val content = item.content?.fold({ it }, { null })
                    if (content is RoomMessageEventContent) {
                        Box(Modifier.padding(16.dp).background(Color.Gray)) {
                            val mentions = if (content is RoomMessageTimelineElementViewModel.TextBased<*>) {
                                (content.mentionsInFormattedBody
                                    ?: content.mentionsInBody)
                                    .map {
                                        it.key to it.value.collectAsState().value
                                    }.sortedByDescending { it.first.first }
                            } else listOf()
                            val message = content.formattedBody ?: content.body
                            val text = formatMessage(message, mentions)
                            val richTextState = rememberSaveable(text, saver = RichTextState.Saver) {
                                RichTextState().apply {
                                    setHtml(text)
                                }
                            }.apply {
                                config.linkColor =
//                                    if (holder.isByMe) MaterialTheme.messengerColors.linkByMe // Inherit link color from Messenger colors
//                                    else MaterialTheme.messengerColors.link
                                    MaterialTheme.messengerColors.linkByMe
                            }
                            Column {
                                BasicRichText(
                                    state = richTextState,
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {

                                            }
                                        )
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
//                                    color = if (isByMe) MaterialTheme.colorScheme.onPrimary
//                                    else MaterialTheme.colorScheme.onSecondary
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                Text(
                                    formatTime(
                                        Instant.fromEpochMilliseconds(item.originTimestamp).toLocalDateTime(timeZone)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.paddingFromBaseline(0.dp),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Text("User Interactions:") // TODO: i18n
            Column {
                userInteractions.filter {
                    interactionFilterByReaction == null || it.reactions.contains(interactionFilterByReaction)
                }.forEach { interaction -> // TODO: use scrolling list
                    val avatarImage = interaction.userInfo.image?.collectAsState(null)?.value
                    Box(Modifier.padding(4.dp)) {
                        Row {
                            Box(Modifier.padding(top = 8.dp, start = 4.dp)) {
                                Avatar(avatarImage, interaction.userInfo.initials ?: "?")
                            }
                            Spacer(Modifier.size(8.dp))
                            Column {
                                Row {
                                    Text(interaction.userInfo.name, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.size(8.dp))
                                    Text(interaction.userInfo.userId.full)
                                }
                                Row {
                                    if (interaction.hasRead) Icon(
                                        Icons.Filled.Done,
                                        i18n.messageBubbleRead(),
                                        Modifier
                                            .size(MaterialTheme.typography.labelSmall.dp)
                                            .padding(start = 2.dp),
                                    )
                                    val reactions = interactionFilterByReaction
                                        ?.let { listOf(it) } ?: interaction.reactions
                                    reactions.forEach { reactionKey ->
                                        Spacer(Modifier.size(8.dp))
                                        Text(reactionKey)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(8.dp))
            Row {
                if (interactionFilterByReaction != null) {
                    Spacer(Modifier.size(8.dp))
                    Button(onClick = {
                        interactionFilterByReaction = null
                    }) {
                        Text("Clear") // TODO: i18n
                    }
                }
                reactionCounts.forEach { reactionCount ->
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = { interactionFilterByReaction = reactionCount.key },
                        border = if (interactionFilterByReaction == reactionCount.key) {
                            ButtonDefaults.outlinedButtonBorder(true)
                        } else null,
                    ) {
                        Text("${reactionCount.key} ${reactionCount.value}")
                    }
                }
            }

        }
    }
}
