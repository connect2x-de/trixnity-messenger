package de.connect2x.trixnity.messenger.viewmodel.spaces

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.store.Room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListSource
import kotlinx.coroutines.flow.Flow

data class RoomListSpace(val userId: UserId, val roomId: RoomId)

internal class SpaceRoomListSource(private val space: RoomListSpace) : RoomListSource {
    override fun getRooms(matrixClient: MatrixClient): Flow<Collection<Room>> =
        getJoinedSpaceChildren(matrixClient, space.roomId, getSpaceParents(matrixClient, space.roomId)) { _, rooms ->
            rooms
        }
}
