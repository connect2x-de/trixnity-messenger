package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.MessageEventContent
import de.connect2x.trixnity.core.model.events.RoomEventContent
import de.connect2x.trixnity.core.model.events.StateEventContent

sealed interface TimelineElementViewModel<C : RoomEventContent> {
    interface State<C : StateEventContent> : TimelineElementViewModel<C>

    interface Message<C : MessageEventContent> : TimelineElementViewModel<C> {
        /** Indicates if the current user is mentioned in the message via ID or a room mention */
        val isMentioned: Boolean
    }

    data object Empty : TimelineElementViewModel<RoomEventContent>
}

fun MessageEventContent.isUserMentioned(userId: UserId): Boolean {
    return mentions?.let { mentions -> mentions.users?.contains(userId) == true || mentions.room == true } ?: false
}
