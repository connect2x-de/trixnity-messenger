package de.connect2x.trixnity.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.EmojiPopup
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionStatus
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionStatus.NotByMe.isByMe

interface MessageReactionsView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        reactionsOpen: MutableState<Boolean>,
        modifier: Modifier,
    )
}

@Composable
fun MessageReactions(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    reactionsOpen: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    DI.get<MessageReactionsView>().create(timelineElementHolderViewModel, reactionsOpen, modifier)
}

class MessageReactionsViewImpl : MessageReactionsView {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        reactionsOpen: MutableState<Boolean>,
        modifier: Modifier,
    ) {
        if (timelineElementHolderViewModel !is TimelineElementHolderViewModel) {
            return
        }
        val reactions = timelineElementHolderViewModel.reactions.collectAsState().value?.byReaction.orEmpty()
        val reactionList =
            remember(reactions) { reactions.entries.sortedByDescending { it.value.reactions.size }.map { it.key } }

        EmojiPopup(
            isOpen = reactionsOpen.value,
            onDismiss = { reactionsOpen.value = false },
            onSelect = {
                reactionsOpen.value = false
                timelineElementHolderViewModel.addReaction(it)
            },
            isByMe = timelineElementHolderViewModel.isByMe,
        )

        MessageReactionList(
            reactionList,
            reactions,
            onAddReaction = timelineElementHolderViewModel::addReaction,
            onRemoveReaction = timelineElementHolderViewModel::removeReaction,
            onOpenReactions = { reactionsOpen.value = true },
            modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageReactionList(
    reactionList: List<String>,
    reactions: Map<String, EventReactions.ByReactionsInfo>,
    onAddReaction: (String) -> Unit,
    onRemoveReaction: (String) -> Unit,
    onOpenReactions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val i18n = DI.get<I18nView>()
    if (reactions.isNotEmpty()) {
        FlowRow(
            modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
        ) {
            for (reaction in reactionList) {
                reactions[reaction]?.let {
                    MessageReactionButton(
                        reaction = reaction,
                        reactionEvents = it,
                        onAddReaction = onAddReaction,
                        onRemoveReaction = { onRemoveReaction(reaction) },
                    )
                }
            }
            MessageAddReactionButton(onClick = onOpenReactions, i18n.reactMessage())
        }
    }
}

@Composable
internal fun MessageReactionDisplay(reaction: String) {
    with(LocalDensity.current) {
        Text(
            text = reaction,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.widthIn(0.dp, LocalTextStyle.current.fontSize.times(10).toDp()),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val buttonModifier = Modifier.sizeIn(minWidth = 54.dp, minHeight = 32.dp, maxHeight = 40.dp)

@Composable
internal fun MessageReactionButton(
    reaction: String,
    reactionEvents: EventReactions.ByReactionsInfo,
    onAddReaction: (reaction: String) -> Unit,
    onRemoveReaction: () -> Unit,
) {
    val highestStatus = reactionEvents.highestStatus
    val count = reactionEvents.reactions.size
    val i18n = DI.get<I18nView>()
    Tooltip({
        Text(reactionEvents.reactions.joinToString { it.sender.name })
    }) {
        ThemedButton(
            onClick = {
                if (highestStatus?.isByMe() == true) {
                    onRemoveReaction()
                } else {
                    onAddReaction(reaction)
                }
            },
            style =
                when (highestStatus) {
                    ReactionStatus.NotByMe -> MaterialTheme.components.reactionButton
                    ReactionStatus.Pending -> MaterialTheme.components.pendingReactionButton
                    ReactionStatus.Sent -> MaterialTheme.components.selectedReactionButton
                    is ReactionStatus.SentError -> MaterialTheme.components.errorReactionButton
                    null -> MaterialTheme.components.reactionButton
                },
            modifier = buttonModifier,
        ) {
            MessageReactionDisplay(reaction)
            Spacer(Modifier.width(MaterialTheme.components.reactionButton.iconSpacing))
            Text(count.toString())
            if (highestStatus is ReactionStatus.SentError) {
                Spacer(Modifier.width(MaterialTheme.components.reactionButton.iconSpacing))
                Tooltip(highestStatus.error ?: i18n.anErrorHasOccurred()) {
                    Icon(Icons.Default.Error, contentDescription = i18n.anErrorHasOccurred())
                }
            }
        }
    }
}

@Composable
internal fun MessageAddReactionButton(onClick: () -> Unit, label: String) {
    ThemedButton(onClick = onClick, style = MaterialTheme.components.reactionButton, modifier = buttonModifier) {
        Icon(
            Icons.Outlined.AddReaction,
            contentDescription = label,
            modifier = Modifier.size(MaterialTheme.components.reactionButton.iconSize),
        )
    }
}
