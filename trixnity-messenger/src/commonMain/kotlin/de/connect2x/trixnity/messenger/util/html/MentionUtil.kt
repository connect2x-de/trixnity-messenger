package de.connect2x.trixnity.messenger.util.html

import io.ktor.http.*
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.util.Reference

fun Reference.toLink(): String = when (this) {
    is Reference.Event -> buildString {
        append("matrix:")
        val finalRoomId = roomId
        if (finalRoomId != null) {
            append("roomid/")
            append(finalRoomId.full.trimStart(RoomId.sigilCharacter))
        }
        append("e/")
        append(eventId.full.trimStart(EventId.sigilCharacter))
        appendParameters(uri)
    }

    is Reference.Room -> buildString {
        append("matrix:roomid/")
        append(roomId.full.trimStart(RoomId.sigilCharacter))
        appendParameters(uri)
    }

    is Reference.RoomAlias -> buildString {
        append("matrix:r/")
        append(roomAliasId.full.trimStart(RoomAliasId.sigilCharacter))
        appendParameters(uri)
    }

    is Reference.User -> buildString {
        append("matrix:u/")
        append(userId.full.trimStart(UserId.sigilCharacter))
        appendParameters(uri)
    }

    is Reference.Link -> uri
}

private fun StringBuilder.appendParameters(uri: String?) {
    if (uri == null) return
    val params = parseUrl(uri)?.parameters ?: return
    if (params.isEmpty()) return
    append('?')
    params.formUrlEncodeTo(this)
}
