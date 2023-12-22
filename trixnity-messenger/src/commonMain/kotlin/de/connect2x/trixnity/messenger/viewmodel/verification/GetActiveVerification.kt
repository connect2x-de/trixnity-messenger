package de.connect2x.trixnity.messenger.viewmodel.verification

import kotlinx.coroutines.flow.StateFlow
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveVerification

// needed to mock ActiveVerification instead of concrete subtypes
// TODO we do not need this anymore (just mock matrixClient.di and inject VerificationService)
interface GetActiveVerification {
    fun activeDeviceVerification(matrixClient: MatrixClient): StateFlow<ActiveVerification?>
    suspend fun activeUserVerification(matrixClient: MatrixClient, timelineEvent: TimelineEvent): ActiveVerification?
}

class GetActiveVerificationImpl : GetActiveVerification {
    override fun activeDeviceVerification(matrixClient: MatrixClient): StateFlow<ActiveVerification?> {
        return matrixClient.verification.activeDeviceVerification
    }

    override suspend fun activeUserVerification(
        matrixClient: MatrixClient,
        timelineEvent: TimelineEvent
    ): ActiveVerification? {
        return matrixClient.verification.getActiveUserVerification(timelineEvent)
    }
}