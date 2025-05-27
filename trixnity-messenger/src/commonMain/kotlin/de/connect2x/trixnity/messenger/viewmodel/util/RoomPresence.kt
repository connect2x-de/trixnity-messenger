package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.Presence

interface RoomPresence {
    operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
    ): Flow<Presence?>
}

class RoomPresenceImpl(
    private val directRoom: DirectRoom,
) : RoomPresence {

    @OptIn(ExperimentalCoroutinesApi::class)
    override operator fun invoke(
        matrixClient: MatrixClient,
        roomId: RoomId,
    ): Flow<Presence?> =
        directRoom.getUsers(matrixClient, roomId).flatMapLatest { directUsers ->
            if (directUsers.isEmpty()) flowOf(null)
            else combine(directUsers.map {
                matrixClient.user.getPresence(it).map { it?.presence }.distinctUntilChanged()
            }) { userPresences ->
                when {
                    userPresences.any { it == Presence.ONLINE } -> Presence.ONLINE
                    userPresences.any { it == Presence.UNAVAILABLE } -> Presence.UNAVAILABLE
                    else -> Presence.OFFLINE
                }
            }
        }
}
