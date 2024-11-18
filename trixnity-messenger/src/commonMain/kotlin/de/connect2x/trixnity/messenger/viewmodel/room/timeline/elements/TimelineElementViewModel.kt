package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.StateEventContent

sealed interface TimelineElementViewModel<C : RoomEventContent> {
    interface State<C : StateEventContent> : TimelineElementViewModel<C>
    interface Message<C : MessageEventContent> : TimelineElementViewModel<C>
    data object Empty : TimelineElementViewModel<RoomEventContent>
}


