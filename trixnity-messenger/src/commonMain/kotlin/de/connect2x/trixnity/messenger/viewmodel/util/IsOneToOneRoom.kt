package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.Membership

/**
 * Provides a boolean flow to determine whether a room is
 * considered a one-to-one room from the client perspective.
 * This means that the operator defined by this interface
 * returns true for all rooms that historically never had
 * more than two users in it.
 */
fun interface IsOneToOneRoom {
    operator fun invoke(matrixClient: MatrixClient, roomId: RoomId): Flow<Boolean>
}

object IsOneToOneRoomImpl : IsOneToOneRoom {
    @OptIn(ExperimentalCoroutinesApi::class)
    override operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId
    ): Flow<Boolean> = matrixClient.room.getById(roomId).flatMapLatest { room ->
        if (room?.isDirect == true) matrixClient.user.getAll(roomId).flatMapLatest { users ->
            if(users.isNotEmpty()) combine(users.values) { combinedUsers ->
                combinedUsers.filter { user -> user?.event?.content?.membership == Membership.JOIN }.size == 2
            } else flowOf(false)
        }
        else flowOf(false)
    }
}
