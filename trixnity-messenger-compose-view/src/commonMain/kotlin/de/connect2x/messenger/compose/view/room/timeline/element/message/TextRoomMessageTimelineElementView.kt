package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInSendMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass


class TextRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.TextBased.Text> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.TextBased.Text> =
        RoomMessageTimelineElementViewModel.TextBased.Text::class

    override suspend fun waitFor(element: RoomMessageTimelineElementViewModel.TextBased.Text) {
        // no-op (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.TextBased.Text,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.TextBased.Text) {
        TextReplyInTimeline(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.TextBased.Text) {
        TextReplyInSendMessage(element)
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.TextBased.Text,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element, {
            showMessageReactions = false
            showContextActionMenu = false
            alwaysShowChatBubbleTail = true
            bubblePadding = { _ -> 0.dp }
        })
    }
}
