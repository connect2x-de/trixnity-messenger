package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.metadata

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.LoadingSpinner
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusContainer
import de.connect2x.trixnity.messenger.compose.view.common.modifier.rovingFocusItem
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.room.settings.TimelineElementMetadataViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.EventReactions
import de.connect2x.trixnity.messenger.viewmodel.util.ReactionStatus

interface TimelineElementMetadataReadersAndReactionsListItemView : TimelineElementMetadataListItemView

class TimelineElementMetadataReadersAndReactionsListItemViewImpl :
    TimelineElementMetadataReadersAndReactionsListItemView {
    @Composable
    override fun ColumnScope.create(
        viewModel: TimelineElementMetadataViewModel,
        elementHistory: List<TimelineElementHolderViewModel>,
        firstElement: TimelineElementHolderViewModel?,
        lastElement: TimelineElementHolderViewModel?,
        scrollState: ScrollState,
        BottomOfStack: Boolean,
        isSinglePane: Boolean,
    ) {
        val reactions = firstElement?.reactions?.collectAsState()?.value
        val readers = firstElement?.readers?.collectAsState()?.value

        if (reactions == null || readers == null) {
            LoadingSpinner()
        } else {
            HorizontalDivider()
            MiddleSpacer()
            ReadersAndReactions(reactions, readers, scrollState, viewModel::openUserProfile)
            SmallSpacer()
        }
    }
}

@Composable
private fun ColumnScope.ReadersAndReactions(
    reactions: EventReactions,
    readers: List<UserInfoElement>,
    parentScrollState: ScrollState,
    onOpenUserProfile: (UserId) -> Unit,
) {
    val i18n = DI.get<I18nView>()
    val state = rememberLazyListState()

    val allReadersAndReactions =
        remember(readers, reactions) {
            readers
                .associate { it.userId to EventReactions.ByUserInfo(mapOf(), it, ReactionStatus.FromOtherAccount) }
                .plus(reactions.byUser)
                .values
                .sortedByDescending { it.reactions.size }
        }
    val hasReadersOrReactions = allReadersAndReactions.isNotEmpty()
    val focusedItem =
        remember(allReadersAndReactions) { mutableStateOf(allReadersAndReactions.firstOrNull()?.sender?.userId) }

    Column(Modifier.heightIn(min = 25.dp, max = 500.dp)) {
        if (hasReadersOrReactions) {
            ThemedListItem(
                style = MaterialTheme.components.settingsItem,
                headlineContent = {
                    Text(
                        i18n.timelineElementMetadataReadersAndReactions(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
            Box {
                LazyColumn(Modifier.rovingFocusContainer(listState = state, focusedItem = focusedItem), state) {
                    items(allReadersAndReactions, { it.sender.userId.full }) { eventReaction ->
                        UserInfo(
                            eventReaction.sender,
                            Modifier.rovingFocusItem(
                                isFocused = { focusedItem.value == eventReaction.sender.userId },
                                onFocus = { focusedItem.value = eventReaction.sender.userId },
                            ),
                            eventReaction.reactions.keys,
                            onOpenUserProfile = onOpenUserProfile,
                        )
                        Spacer(Modifier.height(5.dp))
                    }
                }
                if (state.canScrollForward || state.canScrollBackward) {
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd), state, false)
                }
            }
        } else {
            Text(
                text = i18n.timelineElementMetadataReadersAndReactionsNone(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
