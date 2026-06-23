package de.connect2x.trixnity.messenger.util

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.room.getState
import de.connect2x.trixnity.core.ErrorResponse
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.CreateEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

interface LeaveRoom {
    suspend operator fun invoke(client: MatrixClient, roomId: RoomId, forget: Boolean = true): Result<Unit>
}

class LeaveRoomImpl : LeaveRoom {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.util.LeaveRoomImpl")
    }

    override suspend fun invoke(client: MatrixClient, roomId: RoomId, forget: Boolean): Result<Unit> = runCatching {
        withTimeout(30.seconds) {
            val roomsIds = buildList {
                var currentRoomId: RoomId? = roomId
                while (currentRoomId != null) {
                    add(currentRoomId)
                    currentRoomId =
                        client.room.getState<CreateEventContent>(currentRoomId).first()?.content?.predecessor?.roomId
                }
            }

            log.debug { "start leaving, forgetting and removing rooms $roomsIds" }
            roomsIds.reversed().forEach { roomId ->
                val roomFlow = client.room.getById(roomId)

                val room = roomFlow.first()
                if (room == null) {
                    log.debug {
                        "skip leaving or removing local copy of room $roomId, because it has been already removed"
                    }
                    return@forEach
                }

                if (room.membership != Membership.LEAVE) {
                    log.trace { "leave room $roomId" }
                    val leaveResult = client.api.room.leaveRoom(roomId)
                    val leaveException = leaveResult.exceptionOrNull()

                    // if the room does not exist we want to forget it locally
                    if (leaveException != null && !isRoomNotFoundException(leaveException)) {
                        log.warn(leaveException) {
                            "skip forget room $roomId, because something went wrong (e. g. network error)"
                        }
                        throw leaveException
                    }
                }

                if (forget) {
                    log.trace { "wait for room $roomId to be marked as LEAVE" }
                    withTimeoutOrNull(10.seconds) { roomFlow.filter { it?.membership == Membership.LEAVE }.first() }
                        ?: log.warn { "Exceeded timeout for room membership to switch to leave, forgetting room..." }
                    log.trace { "forget room" }
                    val forgetResult = client.api.room.forgetRoom(roomId)
                    val forgetException = forgetResult.exceptionOrNull()

                    if (forgetException != null && forgetException !is MatrixServerException) {
                        log.warn(forgetException) {
                            "skip removing local copy of room $roomId, because something went wrong (e. g. network error)"
                        }
                        throw forgetException
                    }

                    log.trace { "remove local copy of room $roomId" }
                    client.room.forgetRoom(roomId, true)
                }
            }
        }
    }
}

private fun isRoomNotFoundException(exception: Throwable): Boolean {
    return exception is MatrixServerException && exception.errorResponse is ErrorResponse.NotFound
}
