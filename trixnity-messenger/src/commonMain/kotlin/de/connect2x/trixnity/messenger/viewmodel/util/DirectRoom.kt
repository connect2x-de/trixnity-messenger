package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent

interface DirectRoom {
    fun getUsers(matrixClient: MatrixClient, directRoom: RoomId): Flow<List<UserId>>
}

class DirectRoomImpl : DirectRoom {

    override fun getUsers(matrixClient: MatrixClient, directRoom: RoomId): Flow<List<UserId>> {
        return matrixClient.user.getAccountData<DirectEventContent>().map { directEventContent ->
            directEventContent?.mappings?.let { directMappings ->
                directMappings.entries.filter { (_, rooms) ->
                    rooms?.contains(directRoom) ?: false
                }.map { it.key }
            }?: emptyList()
        }
    }
}