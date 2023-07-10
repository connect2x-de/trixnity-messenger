package de.connect2x.trixnity.messenger.viewmodel.verification

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import org.koin.core.component.get


private val log = KotlinLogging.logger { }

interface SelectVerificationMethodViewModelFactory {
    fun newSelectVerificationViewModel(
        viewModelContext: MatrixClientViewModelContext,
        verificationMethods: Set<VerificationMethod>,
        roomId: RoomId?,
        timelineEventId: EventId?,
        isDeviceVerification: Boolean,
    ): SelectVerificationMethodViewModel {
        return SelectVerificationMethodViewModelImpl(
            viewModelContext, verificationMethods, roomId, timelineEventId, isDeviceVerification,
        )
    }
}

interface SelectVerificationMethodViewModel {
    val verificationMethods: List<Pair<VerificationMethod, String>>
    val hasSelection: Boolean
    fun acceptVerificationMethod(verificationMethod: VerificationMethod)
}

open class SelectVerificationMethodViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    verificationMethods: Set<VerificationMethod>,
    private val roomId: RoomId?,
    private val timelineEventId: EventId?,
    private val isDeviceVerification: Boolean,
) : MatrixClientViewModelContext by viewModelContext, SelectVerificationMethodViewModel {

    private val activeVerifications = get<ActiveVerifications>()

    override val verificationMethods = verificationMethods.map { verificationMethod ->
        when (verificationMethod) {
            is VerificationMethod.Sas -> {
                if (isDeviceVerification) {
                    verificationMethod to i18n.verificationMethodSasDevice()
                } else {
                    verificationMethod to i18n.verificationMethodSasUser()
                }
            }

            is VerificationMethod.Unknown -> {
                verificationMethod to i18n.verificationMethodSasUnknown()
            }
        }
    }
    override val hasSelection = verificationMethods.size > 1

    override fun acceptVerificationMethod(verificationMethod: VerificationMethod) {
        coroutineScope.launch {
            activeVerifications.getActiveVerification(matrixClient, roomId, timelineEventId)
                ?.let { activeVerification ->
                    val currentVerificationState = activeVerification.state.value
                    if (currentVerificationState is ActiveVerificationState.Ready) {
                        log.info { "start verification" }
                        currentVerificationState.start(verificationMethod)
                    } else {
                        log.warn { "accept verification method $verificationMethod, but the current verification state is '$currentVerificationState'" }
                    }
                } ?: log.warn { "cannot get active verification for room $roomId and timeline event $timelineEventId" }
        }
    }

}
