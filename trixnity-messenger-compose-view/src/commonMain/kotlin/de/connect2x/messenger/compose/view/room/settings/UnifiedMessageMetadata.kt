package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey


@Composable
@OptIn(ExperimentalLayoutApi::class)
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel, stackPosition: Int, isSinglePane: Boolean) {
    val i18n = DI.get<I18nView>()
    val scroll = rememberScrollState()
    val edits = viewModel.edits.collectAsState().value
    val reactionCounts = viewModel.reactionCounts.collectAsState().value
    val userInteractions = viewModel.userInteractions.collectAsState().value
    val senderInfo = viewModel.senderInfo.collectAsState().value
    val interactionFilterByReaction = remember { mutableStateOf<ReactionKey?>(null) }
    Box(
        Modifier.fillMaxSize(),
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { viewModel.back() },
                            modifier = Modifier.buttonPointerModifier(),
                        ) {
                            if (isSinglePane || stackPosition > 2) Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                i18n.commonBack()
                            )
                            else Icon(Icons.Default.Close, i18n.commonClose())
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = "Message details".capitalize(Locale.current), // TODO: i18n
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    HorizontalDivider(Modifier.fillMaxWidth().width(1.dp))
//                    if (error != null) {
//                        ErrorView(error)
//                    }
                }
            }
            Box(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
            ) {
                Column(
                    Modifier.verticalScroll(scroll),
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
                                FlowRow {
                                    Text(info.name, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.size(8.dp))
                                    Text(info.userId.full, fontWeight = FontWeight.Light)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
//                    Text(
//                        viewModel.eventId.toString(),
//                        style = MaterialTheme.typography.labelSmall,
//                    )
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
}

@Composable
private fun MessageHistory(
    edits: List<TimelineElementHolderViewModel>,
) {
    Column {
//        listOf(edits.first().element.)
        edits.forEach {
            it.element.collectAsState().value?.let { element ->
                Column(
                    Modifier.padding(end = 8.dp),
                ) {
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
        }.sortedByDescending {
            it.reactions.firstOrNull()?.hashCode()
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
                            interaction.reactions.forEach { reactionKey ->
                                Row {
                                    Text(
                                        reactionKey,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 24.sp),
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
    FlowRow(
        Modifier.padding(start = 8.dp),
    ) {
        reactionCounts.let { map ->
            // This handle case where removal of a reaction causes a broken state.
            interactionFilterByReaction.value
                ?.let { if (map.containsKey(it).not()) map + Pair(it, 0u) else null } ?: map
        }.forEach { reactionCount ->
            val isSelected = interactionFilterByReaction.value == reactionCount.key
            val onClick = {
                if (interactionFilterByReaction.value == reactionCount.key) {
                    interactionFilterByReaction.value = null
                } else interactionFilterByReaction.value = reactionCount.key
            }
            val modifier = Modifier.buttonPointerModifier().padding(6.dp)
            if (isSelected) Button(onClick, modifier) { ReactionButton(reactionCount) }
            else OutlinedButton(onClick, modifier) { ReactionButton(reactionCount) }
        }
    }
}

@Composable
private fun ReactionButton(reactionCount: Map.Entry<ReactionKey, UInt>): Unit = Text(
    "${reactionCount.key} ${reactionCount.value}",
    style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
    modifier = Modifier.paddingFromBaseline(0.dp),
    color = MaterialTheme.colorScheme.onPrimaryContainer,
    maxLines = 1,
)
