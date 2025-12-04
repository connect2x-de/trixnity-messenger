package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
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
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.EmojiPopup
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions


interface MessageReactionsView {
    @Composable
    fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        reactionsOpen: MutableState<Boolean>,
        modifier: Modifier
    )
}

@Composable
fun MessageReactions(
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    reactionsOpen: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    DI.get<MessageReactionsView>().create(timelineElementHolderViewModel, reactionsOpen, modifier)
}

class MessageReactionsViewImpl : MessageReactionsView {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun create(
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
        reactionsOpen: MutableState<Boolean>,
        modifier: Modifier
    ) {
        if (timelineElementHolderViewModel !is TimelineElementHolderViewModel) {
            return
        }
        val reactions = timelineElementHolderViewModel.reactions.collectAsState().value?.byReaction.orEmpty()
        val reactionList = remember(reactions) {
            reactions.entries.sortedByDescending { it.value.size }.map { it.key }
        }

        EmojiPopup(
            isOpen = reactionsOpen.value,
            onDismiss = {
                reactionsOpen.value = false
            },
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
    reactions: Map<String, Set<EventReactions.ByReactionInfo>>,
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
                val reactionEvents = reactions[reaction].orEmpty()
                MessageReactionButton(
                    reaction = reaction,
                    reactionEvents = reactionEvents,
                    count = reactionEvents.size,
                    myReaction = reactionEvents.any { it.isMe },
                    onAddReaction = onAddReaction,
                    onRemoveReaction = { onRemoveReaction(reaction) },
                )
            }
            MessageAddReactionButton(
                onClick = onOpenReactions,
                i18n.reactMessage()
            )
        }
    }
}

@Composable
internal fun MessageReactionDisplay(
    reaction: String,
) {
    with(LocalDensity.current) {
        Text(
            text = reaction,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.widthIn(0.dp, LocalTextStyle.current.fontSize.times(10).toDp()),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private val buttonModifier = Modifier.sizeIn(minWidth = 54.dp, minHeight = 32.dp, maxHeight = 40.dp)

@Composable
internal fun MessageReactionButton(
    reaction: String,
    reactionEvents: Set<EventReactions.ByReactionInfo>,
    count: Int,
    myReaction: Boolean,
    onAddReaction: (reaction: String) -> Unit,
    onRemoveReaction: () -> Unit,
) {
    Tooltip({ Text(reactionEvents.joinToString { it.sender.name }) }) {
        if (myReaction) {
            ThemedButton(
                onClick = { onRemoveReaction() },
                style = MaterialTheme.components.selectedReactionButton,
                modifier = buttonModifier,
            ) {
                MessageReactionDisplay(reaction)
                Spacer(Modifier.width(MaterialTheme.components.reactionButton.iconSpacing))
                Text(count.toString())
            }
        } else {
            ThemedButton(
                onClick = { onAddReaction(reaction) },
                style = MaterialTheme.components.reactionButton,
                modifier = buttonModifier,
            ) {
                MessageReactionDisplay(reaction)
                Spacer(Modifier.width(MaterialTheme.components.reactionButton.iconSpacing))
                Text(count.toString())
            }
        }
    }
}

@Composable
internal fun MessageAddReactionButton(onClick: () -> Unit, label: String) {
    ThemedButton(
        onClick = onClick,
        style = MaterialTheme.components.reactionButton,
        modifier = buttonModifier,
    ) {
        Icon(
            Icons.Outlined.AddReaction,
            contentDescription = label,
            modifier = Modifier.size(MaterialTheme.components.reactionButton.iconSize),
        )
    }
}
