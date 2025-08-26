package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.DirectEventContent

fun interface IsDirectRoom {
    suspend operator fun invoke(matrixClient: MatrixClient, roomId: RoomId): Boolean

    companion object : IsDirectRoom {
        override suspend operator fun invoke(
            matrixClient: MatrixClient,
            roomId: RoomId
        ): Boolean = matrixClient.user.getAccountData<DirectEventContent>()
            .first()
            ?.mappings
            ?.entries
            ?.any { (_, rooms) -> rooms?.contains(roomId) == true }
            ?: false
    }
}
