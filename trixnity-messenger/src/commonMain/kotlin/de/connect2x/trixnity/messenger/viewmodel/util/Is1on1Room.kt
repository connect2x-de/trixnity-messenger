package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.RoomId

/**
 * Provides a boolean flow to determine whether a room is
 * considered a 1-on-1 room from the client perspective.
 * This means that the operator defined by this interface
 * returns true for all rooms that historically never had
 * more than two users in it.
 */
fun interface Is1on1Room {
    operator fun invoke(matrixClient: MatrixClient, roomId: RoomId): Flow<Boolean>
}

class Is1on1RoomImpl(
    private val getRoomUsers: GetRoomUsers
) : Is1on1Room {
    override operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId
    ): Flow<Boolean> = getRoomUsers(matrixClient, roomId).map { users -> users.size <= 2 }
}
