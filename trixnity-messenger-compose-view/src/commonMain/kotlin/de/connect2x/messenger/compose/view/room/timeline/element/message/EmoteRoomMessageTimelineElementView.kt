package de.connect2x.messenger.compose.view.room.timeline.element.message

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.messenger.compose.view.room.timeline.element.message.bubble.MessageBubbleDisplayConfig.Companion.applyPreviewConfig
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInSendMessage
import de.connect2x.messenger.compose.view.room.timeline.element.util.TextReplyInTimeline
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased.Emote
import kotlin.reflect.KClass


class EmoteRoomMessageTimelineElementView : TimelineElementView<Emote> {
    override val supports: KClass<Emote> =
        Emote::class

    override suspend fun waitFor(element: Emote) {
        // NO-OP (has default size)
    }

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Emote,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Emote,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element) { applyPreviewConfig() }
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Emote,
    ) {
        TextReplyInTimeline(holder, element)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Emote,
    ) {
        TextReplyInSendMessage(holder, element)
    }
}
