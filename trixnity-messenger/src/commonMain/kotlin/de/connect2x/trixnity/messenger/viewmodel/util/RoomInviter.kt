package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

interface RoomInviter {
    suspend fun getInviter(matrixClient: MatrixClient, roomId: RoomId): UserId?
}

object RoomInviterImpl : RoomInviter {
    override suspend fun getInviter(matrixClient: MatrixClient, roomId: RoomId): UserId? {
        return withTimeoutOrNull(3.seconds) {
            try {
                val result =
                    matrixClient.room.getState<MemberEventContent>(roomId, stateKey = matrixClient.userId.full)
                        .first { it != null && it.content.membership == Membership.INVITE }
                        ?.sender
                log.debug { "inviter in $roomId is '$result'" }
                result
            } catch (exc: Exception) {
                log.error(exc) { "cannot find an inviter for the user ${matrixClient.userId.full} in the room $roomId" }
                null
            }
        }
    }
}
