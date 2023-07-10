package de.connect2x.trixnity.messenger.viewmodel.verification

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId

interface ActiveVerifications {
    suspend fun getActiveVerification(
        matrixClient: MatrixClient,
        roomId: RoomId?,
        timelineEventId: EventId?,
    ): ActiveVerification?
}

class ActiveVerificationsImpl : ActiveVerifications {
    override suspend fun getActiveVerification(
        matrixClient: MatrixClient,
        roomId: RoomId?,
        timelineEventId: EventId?,
    ): ActiveVerification? {
        return if (timelineEventId == null) {
            matrixClient.verification.activeDeviceVerification
                .filterNotNull()
                .firstOrNull()
        } else {
            roomId?.let {
                matrixClient.room.getTimelineEvent(roomId, timelineEventId).firstOrNull()
                    ?.let { timelineEvent ->
                        matrixClient.verification.getActiveUserVerification(timelineEvent)
                    }
            }
        }
    }
}