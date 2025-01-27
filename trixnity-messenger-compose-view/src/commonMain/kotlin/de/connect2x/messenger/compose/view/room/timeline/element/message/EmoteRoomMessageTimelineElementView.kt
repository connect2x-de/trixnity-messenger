package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInSendMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel
import kotlin.reflect.KClass


class EmoteRoomMessageTimelineElementView : TimelineElementView<RoomMessageTimelineElementViewModel.TextBased.Emote> {
    override val supports: KClass<RoomMessageTimelineElementViewModel.TextBased.Emote> =
        RoomMessageTimelineElementViewModel.TextBased.Emote::class

    override suspend fun waitFor(element: RoomMessageTimelineElementViewModel.TextBased.Emote) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.TextBased.Emote,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createAsMessagePreview(
        holder: BaseTimelineElementHolderViewModel,
        element: RoomMessageTimelineElementViewModel.TextBased.Emote,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element) { applyPreviewConfig() }
    }

    @Composable
    override fun createReplyInTimeline(element: RoomMessageTimelineElementViewModel.TextBased.Emote) {
        TextReplyInTimeline(element)
    }

    @Composable
    override fun createReplyInSendMessage(element: RoomMessageTimelineElementViewModel.TextBased.Emote) {
        TextReplyInSendMessage(element)
    }
}
