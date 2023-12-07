package de.connect2x.trixnity.messenger.viewmodel.room.timeline

import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.isEncrypted
import net.folivo.trixnity.core.model.events.ClientEvent.RoomEvent.MessageEvent
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

interface RelevantTimelineEvents {
    fun isRelevantTimelineEvent(timelineEvent: TimelineEvent?): Boolean {
        val roomEvent = timelineEvent?.event
        val content = timelineEvent?.content?.getOrNull()

        val decryptedMessageEvent =
            roomEvent is MessageEvent && roomEvent.isEncrypted &&
                    content?.let { it is RoomMessageEventContent } ?: true
        val roomMessageEvent = content is RoomMessageEventContent
        val displayedStateEvent =
            (content is MemberEventContent || content is NameEventContent || content is CreateEventContent)

        return decryptedMessageEvent || roomMessageEvent || displayedStateEvent
    }

    companion object : RelevantTimelineEvents
}