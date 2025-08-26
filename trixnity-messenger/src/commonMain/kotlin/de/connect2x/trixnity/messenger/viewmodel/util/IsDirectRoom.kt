package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.RoomId

/**
 * Provides a flow to determine whether a room is a direct room
 * from a member count perspective.
 * If a room has more than 2 members, it is not considered a direct
 * room by the operator defined by this interface.
 * See [RoomUsers] for how users are fetched for a room.
 */
fun interface IsDirectRoom {
    operator fun invoke(matrixClient: MatrixClient, roomId: RoomId): Flow<Boolean>
}

class IsDirectRoomImpl(
    private val roomUsers: RoomUsers
) : IsDirectRoom {
    override operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId
    ): Flow<Boolean> = roomUsers(matrixClient, roomId).map { users -> users.size <= 2 }
}
