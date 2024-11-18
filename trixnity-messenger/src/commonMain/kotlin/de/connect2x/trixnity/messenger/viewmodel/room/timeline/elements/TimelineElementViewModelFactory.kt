package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.RoomEventContent
import kotlin.reflect.KClass

interface TimelineElementViewModelFactory<C : RoomEventContent> {
    val supports: KClass<C>
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        content: C,
        roomId: RoomId,
        eventId: EventIdOrTransactionId,
        onOpenMention: OpenMentionCallback,
        onOpenMedia: OpenMediaCallback,
    ): TimelineElementViewModel<C>?
}
