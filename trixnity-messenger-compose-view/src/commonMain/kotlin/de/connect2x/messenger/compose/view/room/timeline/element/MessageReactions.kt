package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.EmojiPopup
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.flow.MutableStateFlow


private val reactionPadding = PaddingValues(8.dp, 0.dp)

@Composable
internal fun MessageReactionButton(
    reaction: String,
    count: Int,
    myReaction: TimelineElementHolderViewModel.ReactionEvent?,
    onAddReaction: (reaction: String) -> Unit,
    onRemoveReaction: (reaction: TimelineElementHolderViewModel.ReactionEvent) -> Unit,
) {
    if (myReaction != null) {
        FilledTonalButton(onClick = {
            onRemoveReaction(myReaction)
        }, contentPadding = reactionPadding) {
            Text("$reaction $count")
        }
    } else {
        OutlinedButton(onClick = {
            onAddReaction(reaction)
        }, contentPadding = reactionPadding) {
            Text("$reaction $count")
        }
    }
}

@Composable
internal fun MessageReactionList(
    reactions: Map<String, Set<TimelineElementHolderViewModel.ReactionEvent>>,
    onOpenEmojiPicker: () -> Unit,
    onAddReaction: (reaction: String) -> Unit,
    onRemoveReaction: (reaction: TimelineElementHolderViewModel.ReactionEvent) -> Unit,
    alignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier
) {
    val i18n = DI.current.get<I18nView>()
    val reactionList = remember(reactions) {
        reactions.entries.sortedByDescending { it.value.size }.map { it.key }
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, alignment)
    ) {
        items(reactionList, key = { "reaction-$it" }) { reaction ->
            val reactionEvents = reactions[reaction].orEmpty()
            MessageReactionButton(
                reaction = reaction,
                count = reactionEvents.size,
                myReaction = reactionEvents.firstOrNull { it.isMe },
                onAddReaction = onAddReaction,
                onRemoveReaction = onRemoveReaction,
            )
        }
        item(key = "addReaction") {
            OutlinedButton(
                onClick = onOpenEmojiPicker,
                contentPadding = reactionPadding
            ) {
                Icon(
                    Icons.Default.AddReaction,
                    i18n.reactMessage(),
                )
            }
        }
    }
}

@Composable
fun MessageReactions(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    val focusRequester = remember { FocusRequester() }
    val reactionsOpen by remember(timelineElementHolderViewModel) {
        if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
            timelineElementHolderViewModel.reactionsOpen
        } else {
            MutableStateFlow(false)
        }
    }.collectAsState()

    val reactions by remember(timelineElementHolderViewModel) {
        if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
            timelineElementHolderViewModel.reactions
        } else {
            MutableStateFlow(emptyMap())
        }
    }.collectAsState()

    if (timelineElementHolderViewModel is TimelineElementHolderViewModel) {
        EmojiPopup(
            isOpen = reactionsOpen,
            focusRequester = focusRequester,
            onDismiss = {
                timelineElementHolderViewModel.reactionsOpen.value = false
            },
            onSelect = {
                timelineElementHolderViewModel.reactionsOpen.value = false
                timelineElementHolderViewModel.addReaction(it)
            },
            isByMe = roomMessageViewModel.isByMe,
        )
    }

    if (reactions.isNotEmpty() && timelineElementHolderViewModel is TimelineElementHolderViewModel) {
        MessageReactionList(
            reactions = reactions,
            onOpenEmojiPicker = {
                timelineElementHolderViewModel.reactionsOpen.value = true
            },
            onAddReaction = timelineElementHolderViewModel::addReaction,
            onRemoveReaction = timelineElementHolderViewModel::removeReaction,
            alignment = if (roomMessageViewModel.isByMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
