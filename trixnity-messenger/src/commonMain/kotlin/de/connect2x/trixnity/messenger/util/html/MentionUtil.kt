package de.connect2x.trixnity.messenger.util.html

import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.util.Reference
import io.ktor.http.*

fun Reference.toLink(): String =
    when (this) {
        is Reference.Event ->
            buildString {
                append("matrix:")
                val finalRoomId = roomId
                if (finalRoomId != null) {
                    append("roomid/")
                    append(finalRoomId.full.trimStart(RoomId.sigilCharacter))
                    append("/")
                }
                append("e/")
                append(eventId.full.trimStart(EventId.sigilCharacter))
                appendParameters(parameters)
            }

        is Reference.Room ->
            buildString {
                append("matrix:roomid/")
                append(roomId.full.trimStart(RoomId.sigilCharacter))
                appendParameters(parameters)
            }

        is Reference.RoomAlias ->
            buildString {
                append("matrix:r/")
                append(roomAliasId.full.trimStart(RoomAliasId.sigilCharacter))
                appendParameters(parameters)
            }

        is Reference.User ->
            buildString {
                append("matrix:u/")
                append(userId.full.trimStart(UserId.sigilCharacter))
                appendParameters(parameters)
            }

        is Reference.Link -> this.url
    }

private fun StringBuilder.appendParameters(params: Parameters?) {
    if (params != null && !params.isEmpty()) {
        append('?')
        params.formUrlEncodeTo(this)
    }
}
