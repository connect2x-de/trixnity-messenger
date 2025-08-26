package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.Membership

interface RoomUsers {
    fun getUsers(
        matrixClient: MatrixClient,
        roomId: RoomId
    ): Flow<List<UserId>>

    companion object : RoomUsers {
        override fun getUsers(
            matrixClient: MatrixClient,
            roomId: RoomId
        ): Flow<List<UserId>> = matrixClient.user.getAll(roomId) // @formatter:off
            .flattenValues()
            .map { users -> users
                .filter { user -> user.event.content.membership == Membership.JOIN }
                .map { user -> user.userId }
            }
            .distinctUntilChanged() // @formatter:on
    }
}
