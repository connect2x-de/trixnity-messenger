package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.MatrixServerException
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership
import kotlin.time.Duration.Companion.seconds

interface ForgetRoom {
    suspend operator fun invoke(client: MatrixClient, roomId: RoomId, leaveRoom: Boolean = true): Result<Unit>
}

class ForgetRoomImpl : ForgetRoom {
    override suspend fun invoke(client: MatrixClient, roomId: RoomId, leaveRoom: Boolean): Result<Unit> {
        if (leaveRoom) {
            val leaveResult = client.api.room.leaveRoom(roomId)
            if (leaveResult.isFailure) return leaveResult
        }

        return kotlin.runCatching {
            withTimeout(30.seconds) {
                client.room.getById(roomId).filter { it?.membership == Membership.LEAVE }.first()
                val forgetResult = client.api.room.forgetRoom(roomId)
                if (forgetResult.isFailure && forgetResult.exceptionOrNull() is MatrixServerException) {
                    throw requireNotNull(forgetResult.exceptionOrNull())
                }

                client.room.forgetRoom(roomId)
            }
        }
    }
}
