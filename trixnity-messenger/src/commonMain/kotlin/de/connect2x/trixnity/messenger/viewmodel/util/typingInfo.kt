package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.i18n.I18n
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.TypingEventContent

suspend fun typingInfo(
    matrixClient: MatrixClient,
    roomId: RoomId,
    i18n: I18n,
    eventContent: TypingEventContent,
): String? {
    val usersTyping = eventContent.users.filterNot { it == matrixClient.userId }
    return when {
        usersTyping.isEmpty() -> null
        usersTyping.size == 1 -> {
            val username = usersTyping[0].let {
                matrixClient.user.getById(roomId, it).first()?.name
                    ?: it.full
            }
            val isDirect =
                matrixClient.room.getById(roomId).first()?.isDirect ?: false
            when {
                isDirect -> i18n.roomHeaderTypingSingleDirect()
                else -> i18n.roomHeaderTypingSingle(username)
            }
        }

        usersTyping.size < 4 -> {
            val usernames = usersTyping.map {
                matrixClient.user.getById(roomId, it).first()?.name
                    ?: it.full
            }

            i18n.roomHeaderTypingMultiple(
                i18n.commonAnd(
                    usernames.take(usernames.size - 1).joinToString(),
                    usernames.last()
                )
            )
        }

        else -> {
            val usernames = usersTyping.map {
                matrixClient.user.getById(roomId, it).first()?.name
                    ?: it.full
            }
            i18n.roomHeaderTypingMultipleMore(usernames.take(2).joinToString())
        }
    }
}
