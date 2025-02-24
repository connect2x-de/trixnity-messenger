package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInSendMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased.Notice
import kotlin.reflect.KClass

class NoticeRoomMessageTimelineElementView : TimelineElementView<Notice> {
    override val supports: KClass<Notice> =
        Notice::class

    override suspend fun waitFor(element: Notice) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Notice,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: BaseTimelineElementHolderViewModel,
        element: Notice,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(element: Notice) {
        TextReplyInTimeline(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: Notice) {
        TextReplyInSendMessage(element)
    }
}
