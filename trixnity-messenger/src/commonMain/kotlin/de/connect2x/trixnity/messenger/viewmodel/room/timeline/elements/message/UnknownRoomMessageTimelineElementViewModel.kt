package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.RoomMessageEventContent.Unknown
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import kotlin.reflect.KClass

interface UnknownRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<Unknown> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: Unknown,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): RoomMessageTimelineElementViewModel.Unknown? =
        UnknownRoomMessageTimelineElementViewModelImpl(viewModelContext, content, roomId, onOpenMention)

    override val supports: KClass<Unknown>
        get() = Unknown::class

    companion object : UnknownRoomMessageTimelineElementViewModelFactory
}

class UnknownRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: Unknown,
    roomId: RoomId,
    onOpenMention: OpenMentionCallback,
) :
    RoomMessageTimelineElementViewModel.Unknown,
    RoomMessageTimelineElementViewModelImpl<Unknown>(viewModelContext, content, roomId, onOpenMention) {
    override val fallbackBody: String = content.body
}
