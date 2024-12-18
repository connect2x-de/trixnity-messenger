package de.connect2x.trixnity.messenger.viewmodel.verification

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId

interface ActiveVerifications {  // TODO this as part of the DI just adds complexity
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
                matrixClient.verification.getActiveUserVerification(roomId = roomId, timelineEventId)
            }
        }
    }
}

