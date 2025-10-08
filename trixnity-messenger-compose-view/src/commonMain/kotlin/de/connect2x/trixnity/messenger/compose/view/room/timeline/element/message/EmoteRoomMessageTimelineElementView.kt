package de.connect2x.trixnity.messenger.compose.view.room.timeline.element.message

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import de.connect2x.trixnity.messenger.compose.view.room.timeline.element.TimelineElementView
import de.connect2x.trixnity.messenger.compose.view.util.toClipEntry
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased.Emote
import kotlin.reflect.KClass

interface EmoteRoomMessageTimelineElementView : TimelineElementView<Emote>

class EmoteRoomMessageTimelineElementViewImpl : EmoteRoomMessageTimelineElementView {
    override val supports: KClass<Emote> =
        Emote::class

    override suspend fun waitFor(element: Emote) {
        // NO-OP (has default size)
    }

    override fun isFocusable(): Boolean = true

    @Composable
    override fun createInTimeline(
        holder: BaseTimelineElementHolderViewModel,
        element: Emote,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element, isPreview = false)
    }

    @Composable
    override fun createAsPreview(
        holder: TimelineElementHolderViewModel,
        element: Emote,
    ) {
        TextBasedRoomMessageTimelineElementView(holder, element, isPreview = true)
    }

    @Composable
    override fun createReplyInTimeline(
        holder: TimelineElementHolderViewModel,
        element: Emote,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        TextReplyInTimeline(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun createReplyInSendMessage(
        holder: TimelineElementHolderViewModel,
        element: Emote,
        modifier: Modifier,
        interactionSource: MutableInteractionSource,
    ) {
        TextReplyInSendMessage(holder, element, modifier, interactionSource)
    }

    @Composable
    override fun getClipEntry(
        holder: BaseTimelineElementHolderViewModel,
        element: Emote
    ): ClipEntry? = element.toClipEntry()
}
