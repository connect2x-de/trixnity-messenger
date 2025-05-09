package de.connect2x.trixnity.messenger.viewmodel.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership

interface DirectRoom {
    fun getUsers(matrixClient: MatrixClient, directRoom: RoomId): Flow<List<UserId>>
    fun getUsersWithMembership(matrixClient: MatrixClient, directRoom: RoomId): Flow<Map<UserId, Flow<Membership?>>>
}

class DirectRoomImpl : DirectRoom {

    override fun getUsers(matrixClient: MatrixClient, directRoom: RoomId): Flow<List<UserId>> {
        return matrixClient.user.getAccountData<DirectEventContent>().map { directEventContent ->
            directEventContent?.mappings?.let { directMappings ->
                directMappings.entries.filter { (_, rooms) ->
                    rooms?.contains(directRoom) ?: false
                }.map { it.key }
            } ?: emptyList()
        }
    }

    override fun getUsersWithMembership(
        matrixClient: MatrixClient,
        directRoom: RoomId
    ): Flow<Map<UserId, Flow<Membership?>>> = getUsers(matrixClient, directRoom).map {
        it.associateWith { userId -> matrixClient.user.getById(directRoom, userId).map { user -> user?.membership } }
    }

}
