package de.connect2x.messenger.compose.view.room.timeline.element

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.theme.messengerColors
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.RoomMessageViewModel

interface MessageHeaderView {
    @Composable
    fun create(
        roomMessageViewModel: RoomMessageViewModel,
        timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
    )
}

@Composable
fun MessageHeader(
    roomMessageViewModel: RoomMessageViewModel,
    timelineElementHolderViewModel: BaseTimelineElementHolderViewModel,
) {
    DI.get<MessageHeaderView>().create(roomMessageViewModel, timelineElementHolderViewModel)
}

class MessageHeaderViewImpl : MessageHeaderView {
    @Composable
    override fun create(roomMessageViewModel: RoomMessageViewModel, timelineElementHolderViewModel: BaseTimelineElementHolderViewModel) {
        val showSender = roomMessageViewModel.showSender.collectAsState(false)
        val sender = roomMessageViewModel.sender.collectAsState()

        if (showSender.value) {
            Box(
                Modifier
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp)
            ) {
                Text(
                    text = sender.value.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.messengerColors.getUserColor(
                            sender.value.userId
                        )
                    ),
                )
            }
        }
    }
}
