package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlin.reflect.KClass

interface TimelineElementViewModelFactory<C : RoomEventContent> {
    val supports: KClass<C>

    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: C,
        roomId: RoomId,
        eventIdOrTransactionId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
    ): TimelineElementViewModel<C>?
}
