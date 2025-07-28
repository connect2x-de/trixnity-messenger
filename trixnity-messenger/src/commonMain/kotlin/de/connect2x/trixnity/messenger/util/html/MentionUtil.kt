package de.connect2x.trixnity.messenger.util.html

import io.ktor.http.*
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.Mention
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

fun Mention.toLink(): String = when (this) {
    is Mention.Event -> buildString {
        append("matrix:")
        if (roomId != null) {
            append("roomid/")
            append(roomId!!.full.trimStart(RoomId.sigilCharacter))
        }
        append("e/")
        append(eventId.full.trimStart(EventId.sigilCharacter))
        appendParameters(parameters)
    }
    is Mention.Room -> buildString {
        append("matrix:roomid/")
        append(roomId.full.trimStart(RoomId.sigilCharacter))
        appendParameters(parameters)
    }
    is Mention.RoomAlias -> buildString {
        append("matrix:r/")
        append(roomAliasId.full.trimStart(RoomAliasId.sigilCharacter))
        appendParameters(parameters)
    }
    is Mention.User -> buildString {
        append("matrix:u/")
        append(userId.full.trimStart(UserId.sigilCharacter))
        appendParameters(parameters)
    }
}

private fun StringBuilder.appendParameters(params: Parameters?) {
    if (params != null && !params.isEmpty()) {
        append('?')
        params.formUrlEncodeTo(this)
    }
}
