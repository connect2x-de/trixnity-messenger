package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.Presence
import net.folivo.trixnity.core.model.events.m.PresenceEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership

interface UserPresence {
    fun presentEventContentFlow(
        matrixClient: MatrixClient,
        roomId: RoomId,
    ): Flow<PresenceEventContent?>
}

class UserPresenceImpl(
    private val directRoom: DirectRoom,
) : UserPresence {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun presentEventContentFlow(
        matrixClient: MatrixClient,
        roomId: RoomId,
    ): Flow<PresenceEventContent?> {
        return combine(
            matrixClient.user.userPresence,
            directRoom.getUsersWithMembership(matrixClient, roomId).map { users ->
                users.filter { (_, membership) -> membership == Membership.JOIN }.map { (id, _) -> id }
            }
        ) { userPresence, otherUsers ->
            otherUsers.firstOrNull()?.let { userId ->
                userPresence[userId]?.let { presence ->
                    flowOf(presence)
                } ?: flow {
                    emit(PresenceEventContent(presence = Presence.OFFLINE))
                    matrixClient.api.user.getPresence(userId).getOrNull()?.let { presence -> emit(presence) }
                }
            } ?: flowOf(null)
        }.flatMapLatest { it }
    }
}
