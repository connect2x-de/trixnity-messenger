package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.TextBased
import kotlin.reflect.KClass

interface EmoteRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<TextBased.Emote> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: TextBased.Emote,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.TextBased.Emote? =
        EmoteRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
            roomId,
            onOpenMention
        )

    override val supports: KClass<TextBased.Emote>
        get() = TextBased.Emote::class

    companion object : EmoteRoomMessageTimelineElementViewModelFactory
}

class EmoteRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: TextBased.Emote,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) : RoomMessageTimelineElementViewModel.TextBased.Emote,
    TextBasedRoomMessageTimelineElementViewModel<TextBased.Emote>(viewModelContext, content, roomId, onOpenMention)
