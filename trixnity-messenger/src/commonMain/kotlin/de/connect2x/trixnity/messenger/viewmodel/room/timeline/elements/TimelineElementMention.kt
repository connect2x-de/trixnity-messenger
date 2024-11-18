package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.messenger.viewmodel.EventInfoElement
import de.connect2x.trixnity.messenger.viewmodel.RoomInfoElement
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

sealed interface TimelineElementMention {
    data class Room(val room: RoomInfoElement) : TimelineElementMention
    data class User(val user: UserInfoElement) : TimelineElementMention
    data class Event(val event: EventInfoElement, val room: RoomInfoElement) : TimelineElementMention
}
