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

interface LeaveRoom {
    suspend operator fun invoke(client: MatrixClient, roomId: RoomId, forget: Boolean = true): Result<Unit>
}

class LeaveRoomImpl : LeaveRoom {
    override suspend fun invoke(client: MatrixClient, roomId: RoomId, forget: Boolean): Result<Unit> = runCatching {
        withTimeout(30.seconds) {
            val roomFlow = client.room.getById(roomId)
            val room = roomFlow.first()
            if (room == null) {
                client.room.forgetRoom(roomId)
                return@withTimeout
            }

            if (room.membership != Membership.LEAVE) {
                client.api.room.leaveRoom(roomId).getOrThrow()
            }

            if (forget) {
                if (roomFlow.first() == null) {
                    client.room.forgetRoom(roomId)
                    return@withTimeout
                }

                // Server-side and client-side forget
                roomFlow.filter { it?.membership == Membership.LEAVE }.first()
                val forgetResult = client.api.room.forgetRoom(roomId)
                if (forgetResult.isFailure && forgetResult.exceptionOrNull() is MatrixServerException) {
                    throw requireNotNull(forgetResult.exceptionOrNull())
                }
                client.room.forgetRoom(roomId)
            }
        }
    }
}
