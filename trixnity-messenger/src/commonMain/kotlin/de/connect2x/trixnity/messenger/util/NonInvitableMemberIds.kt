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

private val nonInvitableMemberships = setOf(Membership.JOIN, Membership.INVITE, Membership.BAN)

internal fun nonInvitableMemberIds(matrixClient: MatrixClient, roomId: RoomId): Flow<Set<UserId>> {
    return matrixClient.user.getAll(roomId).flatten().mapNotNull { users ->
        users.values.filterNotNull().filter { it.membership in nonInvitableMemberships }.map { it.userId }.toSet()
    }
}
