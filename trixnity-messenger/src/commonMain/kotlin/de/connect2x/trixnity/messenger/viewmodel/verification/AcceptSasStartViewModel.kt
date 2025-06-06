package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification.ActiveSasVerificationMethod
import net.folivo.trixnity.client.verification.ActiveSasVerificationState
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import org.koin.core.component.get


private val log = KotlinLogging.logger { }

interface AcceptSasStartViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId?,
        timelineEventId: EventId?,
    ): AcceptSasStartViewModel {
        return AcceptSasStartViewModelImpl(
            viewModelContext, roomId, timelineEventId,
        )
    }

    companion object : AcceptSasStartViewModelFactory
}

interface AcceptSasStartViewModel {
    fun accept()
}

open class AcceptSasStartViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId?,
    private val timelineEventId: EventId?,
) : MatrixClientViewModelContext by viewModelContext, AcceptSasStartViewModel {

    private val activeVerifications = viewModelContext.get<ActiveVerifications>()

    override fun accept() {
        log.debug { "user accepted SAS start" }
        coroutineScope.launch {
            activeVerifications.getActiveVerification(matrixClient, roomId, timelineEventId)
                ?.let { activeVerification ->
                    log.debug { "start accepting SAS start, active verification: $activeVerification" }
                    val activeVerificationState = activeVerification.state.value
                    log.debug { "active verification state: $activeVerificationState" }
                    if (activeVerificationState is ActiveVerificationState.Start) {
                        val method = activeVerificationState.method
                        log.debug { "active verification method: $method" }
                        if (method is ActiveSasVerificationMethod) {
                            val methodState = method.state.value
                            if (methodState is ActiveSasVerificationState.TheirSasStart) {
                                log.info { "user accepted SAS start" }
                                methodState.accept()
                            } else {
                                log.warn { "active SAS verification is not in state 'TheirSasStart'" }
                            }
                        } else {
                            log.warn { "active verification is not a SAS verification" }
                        }
                    } else {
                        log.warn { "active verification is not in state 'Start'" }
                    }
                }
                ?: log.warn { "cannot get the active verification for room $roomId and timeline event $timelineEventId" }
        }
    }

}
