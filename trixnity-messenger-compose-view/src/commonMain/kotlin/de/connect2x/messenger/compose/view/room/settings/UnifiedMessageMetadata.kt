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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Avatar
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.room.settings.ExtrasPaneHeaderBackButtonType.BACK
import de.connect2x.messenger.compose.view.room.settings.ExtrasPaneHeaderBackButtonType.CLOSE
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementViewSelector
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.MessageUserInteraction
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionKey


@Composable
@OptIn(ExperimentalLayoutApi::class)
fun UnifiedMessageMetadata(viewModel: MessageMetadataViewModel, stackPosition: Int, isSinglePane: Boolean) {
    val scroll = rememberScrollState()
    val edits = viewModel.edits.collectAsState().value
    val reactionCounts = viewModel.reactionCounts.collectAsState().value
    val userInteractions = viewModel.userInteractions.collectAsState().value
    val senderInfo = viewModel.senderInfo.collectAsState().value
    val interactionFilterByReaction = remember { mutableStateOf<ReactionKey?>(null) }
    ExtrasPaneHeader(
        "Message details", // TODO: i18n
        null, // TODO
        { viewModel.back() },
        if (isSinglePane || stackPosition > 2) BACK else CLOSE,
    ) {
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
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
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
private fun ReactionsFilter(
    reactionCounts: Map<ReactionKey, UInt>,
    interactionFilterByReaction: MutableState<ReactionKey?>,
) {
    if (reactionCounts.isEmpty()) return
    val i18n = DI.get<I18nView>()
    val reactionList = reactionCounts.asSequence()
    val tabIndex = interactionFilterByReaction.value?.let { selectedReaction ->
        reactionList.map { it.key }.indexOf(selectedReaction) + 1
    } ?: 0
    // TODO: i18n All + count
    val reactionListWithSum: List<Pair<String, UInt>> =
        listOf(i18n.commonAll() to reactionCounts.map { it.value }.sum()) +
                reactionList.map { it.toPair() }
    HorizontalDivider()
    ScrollableTabRow(
        selectedTabIndex = tabIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 0.dp,
        divider = {},
    ) {
        reactionListWithSum.forEachIndexed { index, (reaction, count) ->
            val isSelected = interactionFilterByReaction.value == reaction
            val onClick = {
                if (index == 0) interactionFilterByReaction.value = null
                else interactionFilterByReaction.value = reaction
            }
            Tab(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.minimumInteractiveComponentSize()
                    .padding(horizontal = 5.dp),
            ) { Text("$reaction $count") }
        }
    }
}
