package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId

/**
 * Try to retrieve the direct conversation partner from the given room.
 */
fun interface GetDirectRoomUser {
    operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId
    ): Flow<UserId?>

    companion object : GetDirectRoomUser {
        @OptIn(ExperimentalCoroutinesApi::class)
        override operator fun invoke(
            matrixClient: MatrixClient,
            roomId: RoomId
        ): Flow<UserId?> = matrixClient.room.getById(roomId).flatMapLatest { room ->
            if (room?.isDirect == true) roomId.getRoomUsers(matrixClient).map { users ->
                if (users.size == 1) (users - matrixClient.userId).first() else null
            } else flowOf(null)
        }
    }
}
