package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.ReactionEventContent
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

internal fun RoomOutboxMessage<*>.isReplacementFor(roomId: RoomId, eventId: EventId) =
    this.roomId == roomId && (content.relatesTo as? RelatesTo.Replace)?.eventId == eventId

internal fun RoomOutboxMessage<*>.isRedactionFor(roomId: RoomId, eventId: EventId) =
    this.roomId == roomId && (content as? RedactionEventContent)?.redacts == eventId

internal fun RoomOutboxMessage<*>.isReactionFor(roomId: RoomId, eventId: EventId) =
    this.roomId == roomId &&
        content is ReactionEventContent &&
        content.relatesTo is RelatesTo.Annotation &&
        content.relatesTo?.eventId == eventId

@OptIn(ExperimentalCoroutinesApi::class)
internal fun getRedactionsFromOutbox(matrixClient: MatrixClient, roomId: RoomId): Flow<Set<EventId>> {
    return matrixClient.room.getOutbox(roomId).flatMapLatest {
        if (it.isEmpty()) {
            (flowOf(emptySet()))
        } else {
            combine(it) { outboxMessages ->
                outboxMessages
                    .mapNotNull { outboxMessage ->
                        val content = outboxMessage?.content
                        if (content is RedactionEventContent) {
                            content.redacts
                        } else {
                            null
                        }
                    }
                    .toSet()
            }
        }
    }
}
