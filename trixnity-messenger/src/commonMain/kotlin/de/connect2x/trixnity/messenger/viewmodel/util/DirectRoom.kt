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
    fun getUser(matrixClient: MatrixClient, directRoom: RoomId): Flow<UserId?>
}

class DirectRoomImpl : DirectRoom {

    override fun getUser(matrixClient: MatrixClient, directRoom: RoomId): Flow<UserId?> {
        return matrixClient.user.getAccountData<DirectEventContent>().map {
            it?.mappings?.let { directMappings ->
                directMappings.entries.find { (_, rooms) ->
                    rooms?.contains(directRoom) ?: false
                }?.key
            }
        }
    }
}