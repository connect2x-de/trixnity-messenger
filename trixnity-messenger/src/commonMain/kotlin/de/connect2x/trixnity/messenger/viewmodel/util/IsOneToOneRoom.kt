package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.RoomId

/**
 * Provides a boolean flow to determine whether a room is
 * considered a one-to-one room from the client perspective.
 * This means that the operator defined by this interface
 * returns true for all rooms that historically never had
 * more than two users in it.
 */
fun interface IsOneToOneRoom {
    operator fun invoke(matrixClient: MatrixClient, roomId: RoomId): Flow<Boolean>

    companion object : IsOneToOneRoom {
        override operator fun invoke(
            matrixClient: MatrixClient,
            roomId: RoomId
        ): Flow<Boolean> = roomId.getRoomUsers(matrixClient).map { users -> users.size <= 2 }
    }
}
