package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.flatten
import de.connect2x.trixnity.client.store.membership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.room.Membership
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

internal fun nonInvitableMemberIds(matrixClient: MatrixClient, roomId: RoomId): Flow<Set<UserId>> {
    return matrixClient.user.getAll(roomId).flatten().mapNotNull { users ->
        users.values.filterNotNull().filterNot { it.membership == Membership.LEAVE }.map { it.userId }.toSet()
    }
}
