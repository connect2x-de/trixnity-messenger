package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import de.connect2x.messenger.compose.view.room.timeline.element.message.formatMessage
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey
import de.connect2x.trixnity.messenger.viewmodel.util.formatDate
import de.connect2x.trixnity.messenger.viewmodel.util.formatTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.originTimestamp
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


//private val log = KotlinLogging.logger {}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel) {
//    val i18n = DI.get<I18nView>()
    val scroll = rememberScrollState()
    val edits = viewModel.edits.collectAsState().value
    val reactionCounts = viewModel.reactionCounts.collectAsState().value
    val userInteractions = viewModel.userInteractions.collectAsState().value
    val senderInfo = viewModel.senderInfo.collectAsState().value
    val interactionFilterByReaction = remember { mutableStateOf<ReactionKey?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column {
            Header(viewModel::back, "we need to go back")
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
                    .verticalScroll(scroll)
            ) {
                Spacer(Modifier.size(8.dp))
                Text("Message Sender:") // TODO: i18n
                senderInfo?.let { info ->
                    val avatarImage = info.image?.collectAsState(null)?.value
                    Box(Modifier.padding(4.dp)) {
                        Row {
                            Box(Modifier.padding(top = 6.dp, start = 6.dp)) {
                                Avatar(avatarImage, info.initials ?: "?")
                            }
                            Spacer(Modifier.size(8.dp))
                            Column {
                                Text(info.name, fontWeight = FontWeight.Bold)
                                Text(info.userId.full, fontWeight = FontWeight.Light)
                            }
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    viewModel.eventId.toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text("Editing history:") // TODO: i18n
                MessageHistory(edits.sortedBy { it.originTimestamp }.reversed())
                Spacer(Modifier.size(8.dp))
                Text("Message read by:") // TODO: i18n
                UserInteractions(userInteractions, interactionFilterByReaction.value)
                Spacer(Modifier.size(8.dp))
                ReactionsFilter(reactionCounts, interactionFilterByReaction)
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ReactionsFilter(
    reactionCounts: Map<ReactionKey, UInt>,
    interactionFilterByReaction: MutableState<ReactionKey?>,
) {
    FlowRow(Modifier.padding(start = 8.dp)) {
        if (interactionFilterByReaction.value != null) {
            Button(onClick = {
                interactionFilterByReaction.value = null
            }) {
                Text("Clear") // TODO: i18n
            }
            Spacer(Modifier.size(8.dp))
        }
        reactionCounts.forEach { reactionCount ->
            val isSelected = interactionFilterByReaction.value == reactionCount.key
            Button(
                onClick = { interactionFilterByReaction.value = reactionCount.key },
                border = if (isSelected) ButtonDefaults.outlinedButtonBorder(true) else null,
            ) {
                Text(
                    "${reactionCount.key} ${reactionCount.value}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.paddingFromBaseline(0.dp),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun UserInteractions(
    userInteractions: List<MessageUserInteraction>,
    interactionFilterByReaction: ReactionKey?,
) {
    Column {
        userInteractions.filter {
            interactionFilterByReaction == null || it.reactions.contains(interactionFilterByReaction)
        }.forEach { interaction ->
            val avatarImage = interaction.userInfo.image?.collectAsState(null)?.value
            Box(Modifier.padding(4.dp)) {
                Row {
                    Box(Modifier.padding(top = 6.dp, start = 6.dp)) {
                        Avatar(avatarImage, interaction.userInfo.initials ?: "?")
                    }
                    Spacer(Modifier.size(8.dp))
                    Column {
                        FlowRow {
                            Text(interaction.userInfo.name, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.size(8.dp))
                            Text(interaction.userInfo.userId.full, fontWeight = FontWeight.Light)
                        }
                        FlowRow {
//                                    if (interaction.hasRead) {
//                                        Text(
//                                            "(seen)",
//                                            style = MaterialTheme.typography.labelSmall,
//                                            modifier = Modifier.paddingFromBaseline(0.dp),
//                                            maxLines = 1,
//                                        ) // TODO: i18n
//                                        Spacer(Modifier.size(6.dp))
//                                    }
                            interaction.reactions.forEach { reactionKey ->
                                Row {
                                    Text(
                                        reactionKey,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.paddingFromBaseline(0.dp),
                                        maxLines = 1,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageHistory(
    edits: List<TimelineEvent>,
) {
    val timeZone = DI.get<TimeZone>()
    Column {
        edits.forEach { item ->
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
                        val time = formatTime(
                            Instant.fromEpochMilliseconds(item.originTimestamp).toLocalDateTime(timeZone)
                        )
                        val date = formatDate(
                            Instant.fromEpochMilliseconds(item.originTimestamp).toLocalDateTime(timeZone)
                        )
                        Text(
                            "$date - $time",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.paddingFromBaseline(0.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
