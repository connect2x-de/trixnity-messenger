package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.client.verification.ActiveVerification
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.RoomId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull

interface ActiveVerifications { // TODO this as part of the DI just adds complexity
    /**
     * Returns whether there is an active device or user verification.
     *
     * @param matrixClient
     * @param roomId only needed for user verification; for device verification should be `null`
     * @param timelineEventId only needed for user verification; for device verification should be `null`
     */
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
            matrixClient.verification.activeDeviceVerification.filterNotNull().firstOrNull()
        } else {
            roomId?.let { matrixClient.verification.getActiveUserVerification(roomId = roomId, timelineEventId) }
        }
    }
}
