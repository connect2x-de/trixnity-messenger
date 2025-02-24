package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased.Text
import kotlin.reflect.KClass

class TextRoomMessageTimelineElementView : TimelineElementView<Text> {
    override val supports: KClass<Text> =
        Text::class

    override suspend fun waitFor(element: Text) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Text,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Text,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Text,
    ) {
        TextReplyInTimeline(holder, element)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Text,
    ) {
        TextReplyInSendMessage(holder, element)
    }
}
