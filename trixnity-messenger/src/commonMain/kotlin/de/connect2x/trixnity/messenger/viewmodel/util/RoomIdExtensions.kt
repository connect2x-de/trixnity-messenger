package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.UserService
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.DirectEventContent

/**
 * Retrieve all users which are historically part of this room using the given client instance.
 * Includes the user ID of the current user.
 *
 * This function will first attempt resolving the room users through [DirectEventContent],
 * if that fails **it will call [UserService.getAll] and try to resolve them that way**
 * as a means of fallback when the given rooms [net.folivo.trixnity.client.store.Room.isDirect] flag
 * is not true.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun RoomId.getRoomUsers(
    matrixClient: MatrixClient
): Flow<List<UserId>> = matrixClient.room.getById(this).filterNotNull().flatMapLatest { room ->
    if (room.isDirect) matrixClient.user.getAccountData<DirectEventContent>().map { content ->
        content?.mappings?.keys?.toList() ?: emptyList()
    } else matrixClient.user.getAll(this)
        .flattenValues()
        .map { users -> users.map { user -> user.userId } }
}
