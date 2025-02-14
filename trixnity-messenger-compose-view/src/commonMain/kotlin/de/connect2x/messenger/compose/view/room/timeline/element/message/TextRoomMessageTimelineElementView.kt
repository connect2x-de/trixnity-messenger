package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInSendMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
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
        holder: BaseTimelineElementHolderViewModel,
        element: Text,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element) { applyPreviewConfig() }
    }

    @Composable
    override fun createReplyInTimeline(element: Text) {
        TextReplyInTimeline(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: Text) {
        TextReplyInSendMessage(element)
    }
}
