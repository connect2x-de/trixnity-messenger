package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.RoomAliasId
import net.folivo.trixnity.core.model.RoomId

interface RoomAlias {
    fun getRoomId(roomAliasId: RoomAliasId, matrixClient: MatrixClient): Flow<RoomId>
    suspend fun setRoomAlias(roomId: RoomId?, roomAliasId: RoomAliasId, matrixClient: MatrixClient)
}

open class RoomAliasImpl() : RoomAlias {
    override fun getRoomId(roomAliasId: RoomAliasId, matrixClient: MatrixClient): Flow<RoomId> {
        return flow {
            // to the reviewer: I think the function was just misnamed.
            // Would wait until Benedict is out of holidays though
            matrixClient.api.room.getRoomAlias(roomAliasId).getOrNull()?.roomId
        }
    }

    override suspend fun setRoomAlias(roomId: RoomId?, roomAliasId: RoomAliasId, matrixClient: MatrixClient) {
        if (roomId == null) {
            matrixClient.api.room.deleteRoomAlias(roomAliasId)
        } else {
            matrixClient.api.room.setRoomAlias(roomId, roomAliasId)
        }
    }
}
