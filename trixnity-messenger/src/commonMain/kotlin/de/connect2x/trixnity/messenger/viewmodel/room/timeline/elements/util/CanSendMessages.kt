package de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.PowerLevelsEventContent

fun canSendMessages(
    matrixClient: MatrixClient,
    roomId: RoomId,
): Flow<Boolean> {
    return matrixClient.room.getState<PowerLevelsEventContent>(roomId)
        .filterNotNull()
        .map { powerLevels ->
            val content = powerLevels.content
            val eventsDefault = content.eventsDefault
            val ownPowerLevel =
                content.users[matrixClient.userId] ?: content.usersDefault
            ownPowerLevel >= eventsDefault
        }
}