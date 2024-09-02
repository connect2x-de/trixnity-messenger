package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.room.timeline.RoomContent
import de.connect2x.messenger.compose.view.room.timeline.RoomHeader
import de.connect2x.messenger.compose.view.room.timeline.TimelineElement
import de.connect2x.messenger.compose.view.room.timeline.element.MessageContainer
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.PreviewTimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OutboxElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTextMessageViewModel1
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.PreviewTimelineElementViewModel1
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomContentPreview() {
    InitMessengerPreview {
        RoomContent(timelineViewModel = PreviewTimelineViewModel())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
fun RoomHeaderPreview() {
    InitMessengerPreview {
        val timelineViewModel = PreviewTimelineViewModel()
        RoomHeader(timelineViewModel.roomHeaderViewModel)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun TimelineElementPreview() {
    InitMessengerPreview {
        TimelineElement(PreviewTimelineElementViewModel1(), PreviewTimelineViewModel())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun MessageContainerPreview() {
    InitMessengerPreview {
        MessageContainer(PreviewTextMessageViewModel1(), PreviewTimelineElementViewModel1())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun MessageContainerOutboxWithErrorPreview() {
    InitMessengerPreview {
        MessageContainer(PreviewTextMessageViewModel1(),
            object : OutboxElementHolderViewModel {
                override val canAbortSend: StateFlow<Boolean> = MutableStateFlow(true)
                override val canRetrySend: StateFlow<Boolean> = MutableStateFlow(true)
                override val key: String = "outbox-1"
                override val sendError: StateFlow<String?> =
                    MutableStateFlow("missing permissions to send the message")
                override val timelineElementViewModel: StateFlow<BaseTimelineElementViewModel?> =
                    MutableStateFlow(PreviewTextMessageViewModel1())
                override val transactionId: String = "outbox-1"

                override fun abortSend() {
                }

                override fun retrySend() {
                }
            })
    }
}
