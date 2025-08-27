package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.membership
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.room.Membership

fun interface RoomPresence {
    operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
    ): Flow<Presence?>
}

class RoomPresenceImpl(
    val getRoomUsers: GetRoomUsers,
) : RoomPresence {
    @OptIn(ExperimentalCoroutinesApi::class)
    override operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
    ): Flow<Presence?> =
        matrixClient.room.getById(roomId).map { room -> room?.isDirect == true }.flatMapLatest { isDirect ->
            if (isDirect)
                getRoomUsers(matrixClient, roomId)
                    .map { it - matrixClient.userId }
                    .flatMapLatest { users ->
                        if (users.isEmpty()) flowOf(emptyList())
                        else combine(users.map { directUser ->
                            matrixClient.user.getById(roomId, directUser)
                                .map { roomUser -> if (roomUser != null) roomUser.userId to roomUser.membership else null }
                                .distinctUntilChanged()
                        }) { directUsersWithMembership ->
                            directUsersWithMembership
                                .filterNotNull()
                                .filter { it.second == Membership.JOIN }
                                .map { it.first }
                        }
                    }
                    .flatMapLatest { users ->
                        if (users.isEmpty()) flowOf(null)
                        else combine(users.map {
                            matrixClient.user.getPresence(it)
                                .map { userPresence -> userPresence?.presence }
                                .distinctUntilChanged()
                        }) { userPresences ->
                            when {
                                userPresences.any { it == Presence.ONLINE } -> Presence.ONLINE
                                userPresences.any { it == Presence.UNAVAILABLE } -> Presence.UNAVAILABLE
                                else -> Presence.OFFLINE
                            }
                        }
                    }
            else flowOf(null)
        }
}
