package de.connect2x.messenger.compose.view.room.settings

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey


//private val log = KotlinLogging.logger {}

@Composable
//@OptIn(ExperimentalLayoutApi::class)
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
            Header(viewModel::back, "we need to go back") // TODO: text & i18n
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
                MessageHistory(edits.sortedBy { "${it.formattedDate} - ${it.formattedTime}" }.reversed())
                Spacer(Modifier.size(8.dp))
                Text("Message read by:") // TODO: i18n
                UserInteractions(userInteractions, interactionFilterByReaction.value)
                Spacer(Modifier.size(8.dp))
                // TODO: make reactions filter always visible
                ReactionsFilter(reactionCounts, interactionFilterByReaction)
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun MessageHistory(
    edits: List<TimelineElementHolderViewModel>,
) {
    Column {
        edits.forEach {
            it.element.collectAsState().value?.let { element ->
                Column(
                    Modifier
//                        .background(Color.LightGray)
                        .padding(end = 8.dp)
                ) {
//                    Text("-> ${item.eventId}:")
//                    TimelineElementSelector(item, element)
//                with(DI.get<TimelineElementViewSelector>()) { createReplyInTimeline(element) }
                    with(DI.get<TimelineElementViewSelector>()) { createAsMessagePreview(it, element) }
                }
                Spacer(Modifier.size(8.dp))
            }
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
@OptIn(ExperimentalLayoutApi::class)
private fun ReactionsFilter(
    reactionCounts: Map<ReactionKey, UInt>,
    interactionFilterByReaction: MutableState<ReactionKey?>,
) {
    FlowRow(Modifier.padding(start = 8.dp)) {
//        if (interactionFilterByReaction.value != null) {
//            Button(onClick = {
//                interactionFilterByReaction.value = null
//            }) {
//                Text("Clear") // TODO: i18n
//            }
//            Spacer(Modifier.size(8.dp))
//        }
        reactionCounts.let { map ->
            // Handle case where removal of a reaction causes a broken state.
            interactionFilterByReaction.value
                ?.let { if (map.containsKey(it).not()) map + Pair(it, 0u) else null } ?: map
        }.forEach { reactionCount ->
            val isSelected = interactionFilterByReaction.value == reactionCount.key
            Button(
                onClick = {
                    if (interactionFilterByReaction.value == reactionCount.key) {
                        interactionFilterByReaction.value = null
                    } else interactionFilterByReaction.value = reactionCount.key
                },
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
