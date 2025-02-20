package de.connect2x.trixnity.messenger.util

import net.folivo.trixnity.core.model.events.MessageEventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.EncryptedMessageEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


interface RelevantTimelineEvents {
    fun isRelevantTimelineEvent(content: RoomEventContent): Boolean {
        val isReplace =
            content is MessageEventContent && content.relatesTo is RelatesTo.Replace
        val isMessage = content is RoomMessageEventContent || content is EncryptedMessageEventContent

        return !isReplace && isMessage
    }

    companion object : RelevantTimelineEvents
}
