package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.MemberEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

interface RoomInviter {
    suspend fun getInviter(matrixClient: MatrixClient, roomId: RoomId): UserId?
}

object RoomInviterImpl : RoomInviter {
    private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.util.RoomInviterImpl")

    override suspend fun getInviter(matrixClient: MatrixClient, roomId: RoomId): UserId? {
        return withTimeoutOrNull(3.seconds) {
            try {
                val result =
                    matrixClient.room
                        .getState<MemberEventContent>(roomId, stateKey = matrixClient.userId.full)
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
