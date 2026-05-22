package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.StateEventContent

sealed interface TimelineElementViewModel<C : RoomEventContent> {
    interface State<C : StateEventContent> : TimelineElementViewModel<C>

    interface Message<C : MessageEventContent> : TimelineElementViewModel<C>

    data object Empty : TimelineElementViewModel<RoomEventContent>
}
