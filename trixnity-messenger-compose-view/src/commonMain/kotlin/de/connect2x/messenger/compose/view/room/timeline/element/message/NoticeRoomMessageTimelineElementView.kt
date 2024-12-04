package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInSendMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass

class NoticeRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.TextBased.Notice> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.TextBased.Notice> =
        RoomMessageTimelineElementViewModel.TextBased.Notice::class

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.TextBased.Notice
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.TextBased.Notice) {
        TextReplyInTimeline(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.TextBased.Notice) {
        TextReplyInSendMessage(element)
    }
}
