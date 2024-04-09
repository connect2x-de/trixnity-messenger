package de.connect2x.trixnity.messenger.viewmodel

import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.events.ClientEvent
import net.folivo.trixnity.core.model.events.RoomEventContent

data class EventInfoElement(
    var eventId: EventId,
    val event: ClientEvent.RoomEvent<*>,
    val content: RoomEventContent?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EventInfoElement

        if (eventId != other.eventId) return false
        if (event != other.event) return false
        if (content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}

fun TimelineEvent.toEventInfoElement(): EventInfoElement {
    return EventInfoElement(
        eventId = this.eventId,
        event = this.event,
        content = this.content?.getOrNull()
    )
}
