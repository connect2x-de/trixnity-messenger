package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RedactedMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import kotlinx.coroutines.flow.MutableStateFlow

interface MessageContainerView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun MessageContainer(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    DI.get<MessageContainerView>().create(roomMessageViewModel, timelineElementHolderViewModel)
}

class MessageContainerViewImpl : MessageContainerView {
    @Composable
    override fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    ) {
        val redactionInProgress by remember {
            if (timelineElementHolderViewModel is TimelineElementHolderViewModel)
                timelineElementHolderViewModel.redactionInProgress
            else MutableStateFlow(false)
        }.collectAsState()
        val topPadding = if (roomMessageViewModel.showBigGap) 10.dp else 3.dp

        BoxWithConstraints(
            Modifier.fillMaxWidth()
        ) {
            val padding =
                (if (maxWidth < 400.dp) 20.dp else 80.dp) - (if (redactionInProgress) 16.dp else 0.dp)
            Column(
                modifier = Modifier.run {
                    if (roomMessageViewModel.isByMe) padding(start = padding, top = topPadding)
                        .align(Alignment.CenterEnd)
                    else padding(end = padding, top = topPadding)
                },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (roomMessageViewModel.isByMe) Alignment.End else Alignment.Start,
            ) {
                Row {
                    if (redactionInProgress) {
                        RedactionInProgress()
                    }
                    MessageBubble(
                        roomMessageViewModel,
                        timelineElementHolderViewModel,
                    )
                }
                if (roomMessageViewModel !is RedactedMessageViewModel) {
                    MessageReactions(
                        roomMessageViewModel,
                        timelineElementHolderViewModel,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RedactionInProgress() {
    val i18n = DI.get<I18nView>()
    Box(Modifier.size(16.dp).padding(2.dp)) {
        Icon(Icons.Default.AutoDelete, i18n.messageBubbleBeingDeleted())
    }
}
