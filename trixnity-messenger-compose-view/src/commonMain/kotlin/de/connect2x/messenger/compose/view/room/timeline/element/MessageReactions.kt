package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.EmojiPopup
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel

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

        val i18n = DI.current.get<I18nView>()

        val reactions by remember(timelineElementHolderViewModel) {
            timelineElementHolderViewModel.reactions
        }.collectAsState()

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
                        myReaction = reactionEvents.firstOrNull { it.isMe },
                        onAddReaction = timelineElementHolderViewModel::addReaction,
                        onRemoveReaction = timelineElementHolderViewModel::removeReaction,
                    )
                }
                MessageAddReactionButton(
                    onClick = {
                        reactionsOpen.value = true
                    },
                    i18n.reactMessage()
                )
            }
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

private val buttonPadding = PaddingValues(12.dp, 4.dp)
private val buttonModifier = Modifier.buttonPointerModifier()
    .defaultMinSize(minWidth = 54.dp, minHeight = 32.dp)
    .heightIn(max = 40.dp)

@Composable
internal fun MessageReactionButton(
    reaction: String,
    reactionEvents: Set<TimelineElementHolderViewModel.ReactionEvent>,
    count: Int,
    myReaction: TimelineElementHolderViewModel.ReactionEvent?,
    onAddReaction: (reaction: String) -> Unit,
    onRemoveReaction: (reaction: TimelineElementHolderViewModel.ReactionEvent) -> Unit,
) {
    Tooltip({ TooltipText(reactionEvents.joinToString { it.sender.name }) }) {
        if (myReaction != null) {
            FilledTonalButton(
                onClick = { onRemoveReaction(myReaction) },
                contentPadding = buttonPadding,
                modifier = buttonModifier,
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            ) {
                MessageReactionDisplay(reaction)
                Spacer(Modifier.width(4.dp))
                Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            OutlinedButton(
                onClick = { onAddReaction(reaction) },
                contentPadding = buttonPadding,
                modifier = buttonModifier,
            ) {
                MessageReactionDisplay(reaction)
                Spacer(Modifier.width(4.dp))
                Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun MessageAddReactionButton(onClick: () -> Unit, label: String) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = buttonPadding,
        modifier = buttonModifier,
    ) {
        Icon(
            Icons.Outlined.AddReaction,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
