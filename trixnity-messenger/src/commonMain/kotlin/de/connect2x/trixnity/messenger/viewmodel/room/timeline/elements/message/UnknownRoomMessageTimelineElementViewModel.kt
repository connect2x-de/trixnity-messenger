package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.EventIdOrTransactionId
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMediaCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.OpenMentionCallback
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModelFactory
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent.Unknown
import kotlin.reflect.KClass

interface UnknownRoomMessageTimelineElementViewModelFactory : TimelineElementViewModelFactory<Unknown> {
    override fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: Unknown,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback,
    ): RoomMessageTimelineElementViewModel.Unknown? =
        UnknownRoomMessageTimelineElementViewModelImpl(
            viewModelContext,
            content,
        )

    override val supports: KClass<Unknown>
        get() = Unknown::class

    companion object : UnknownRoomMessageTimelineElementViewModelFactory
}

class UnknownRoomMessageTimelineElementViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    content: Unknown,
) : RoomMessageTimelineElementViewModel.Unknown, MatrixClientViewModelContext by viewModelContext {
    override val fallbackBody: String = content.body
}
