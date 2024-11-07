package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.NameEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

interface RelevantTimelineEvents {
    fun isRelevantTimelineEvent(content: RoomEventContent): Boolean {
        val isReplace =
            content is MessageEventContent && content.relatesTo is RelatesTo.Replace
        val isMessage = content is RoomMessageEventContent || content is EncryptedMessageEventContent
        val isStateEvent = content is MemberEventContent || content is NameEventContent || content is CreateEventContent

        return !isReplace && (isMessage || isStateEvent)
    }

    companion object : RelevantTimelineEvents
}
