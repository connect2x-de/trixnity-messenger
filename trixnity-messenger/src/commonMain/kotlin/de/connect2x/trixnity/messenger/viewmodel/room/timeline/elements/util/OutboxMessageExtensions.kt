package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import de.connect2x.trixnity.client.store.RoomOutboxMessage
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.events.m.RelatesTo
import de.connect2x.trixnity.core.model.events.m.room.RedactionEventContent

internal fun RoomOutboxMessage<*>.isReplacementFor(roomId: RoomId,eventId: EventId) =
    roomId==roomId&&(content.relatesTo as? RelatesTo.Replace)?.eventId == eventId

internal fun RoomOutboxMessage<*>.isRedactionFor(roomId: RoomId,eventId: EventId) =
    roomId==roomId&&(content as? RedactionEventContent)?.redacts == eventId
