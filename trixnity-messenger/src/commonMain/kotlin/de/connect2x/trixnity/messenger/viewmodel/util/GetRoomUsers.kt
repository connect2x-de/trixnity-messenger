package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent

/**
 * Returns a list of all users in the given room.
 * Uses direct event mappings for a fast path when [net.folivo.trixnity.client.store.Room.isDirect] is true.
 */
fun interface GetRoomUsers {
    operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId
    ): Flow<List<UserId>>

    companion object : GetRoomUsers {
        @OptIn(ExperimentalCoroutinesApi::class)
        override operator fun invoke(
            matrixClient: MatrixClient,
            roomId: RoomId
        ): Flow<List<UserId>> = matrixClient.room.getById(roomId).filterNotNull().flatMapLatest { room ->
            if (room.isDirect) matrixClient.user.getAccountData<DirectEventContent>().map { content ->
                content?.mappings?.entries?.filter { (_, rooms) ->
                    rooms?.contains(roomId) ?: false
                }?.map { it.key } ?: emptyList()
            }
            else matrixClient.user.getAll(roomId)
                .flattenValues()
                .map { users -> users.map { user -> user.userId } + matrixClient.userId }
        }
    }
}
