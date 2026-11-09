package de.connect2x.trixnity.messenger.viewmodel.verification.v2

import de.connect2x.trixnity.client.key
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.eventId
import de.connect2x.trixnity.client.store.roomId
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.client.verification.ActiveDeviceVerification
import de.connect2x.trixnity.client.verification.ActiveSasVerificationMethod
import de.connect2x.trixnity.client.verification.ActiveSasVerificationState
import de.connect2x.trixnity.client.verification.ActiveVerification
import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.core.model.EventId
import de.connect2x.trixnity.core.model.ProfileField.DisplayName
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationMethod
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.BackHandler
import de.connect2x.trixnity.messenger.util.GetAccountProfileDisplayName
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.emojisWithTranslation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface VerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseVerification: () -> Unit,
        onRedoSelfVerification: () -> Unit,
        roomId: RoomId?,
        timelineEventId: EventId?,
        onStepChange: (ActiveVerificationState) -> Unit,
        onSasStepChange: (ActiveSasVerificationState) -> Unit,
        getAccountProfileDisplayName: GetAccountProfileDisplayName,
    ): VerificationViewModel {
        return VerificationViewModelImpl(
            viewModelContext,
            onCloseVerification,
            onRedoSelfVerification,
            roomId,
            timelineEventId,
            onStepChange,
            onSasStepChange,
            getAccountProfileDisplayName,
        )
    }

    companion object : VerificationViewModelFactory
}

interface VerificationViewModel {
    val ourUserId: UserId
    val theirUserId: StateFlow<UserId?>
    val ourDisplayName: StateFlow<String>
    val theirDisplayName: StateFlow<String?>
    val ourDeviceDisplayName: StateFlow<String>
    val theirDeviceDisplayName: StateFlow<String?>
    val activeVerificationState: StateFlow<ActiveVerificationState?>

    val emojis: StateFlow<List<Pair<String, Map<String, String>>>?>

    val isFromOwnAccount: StateFlow<Boolean?>
    val decimals: StateFlow<List<Int>?>

    val verificationMethods: StateFlow<List<Pair<VerificationMethod, String>>?>

    fun acceptEmojisOrNumbers()

    fun declineEmojisOrNumbers()

    fun acceptRequest()

    fun acceptSasVerification()

    fun acceptVerificationMethod(verificationMethod: VerificationMethod)

    fun ok()

    fun cancel()
}

open class VerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseVerification: () -> Unit,
    private val onRedoSelfVerification: () -> Unit,
    private val roomId: RoomId?,
    private val timelineEventId: EventId?,
    private val onStateChange: (ActiveVerificationState) -> Unit,
    private val onSasStateChange: (ActiveSasVerificationState) -> Unit,
    getAccountProfileDisplayName: GetAccountProfileDisplayName,
) : MatrixClientViewModelContext by viewModelContext, VerificationViewModel {
    private val activeVerification = MutableStateFlow<ActiveVerification?>(null)

    private val theirDeviceId = activeVerification.map { it?.theirDeviceId }
    override val theirUserId = activeVerification.map { it?.theirUserId }.stateIn(coroutineScope, Eagerly, null)

    override val ourUserId: UserId = userId

    override val ourDisplayName: StateFlow<String> =
        getAccountProfileDisplayName
            .fromMatrixClient(matrixClient)
            .stateIn(coroutineScope, WhileSubscribed(), userId.full)

    override val ourDeviceDisplayName: StateFlow<String> =
        flow { emit(matrixClient.api.device.getDevice(matrixClient.deviceId).fold({ it }, { null })) }
            .map { it?.displayName ?: matrixClient.deviceId }
            .stateIn(coroutineScope, WhileSubscribed(), matrixClient.deviceId)

    override val theirDeviceDisplayName: StateFlow<String?> =
        theirDeviceId
            .map { theirDeviceId ->
                theirDeviceId?.let {
                    matrixClient.api.device.getDevice(it).fold({ it.displayName }, { null }) ?: theirDeviceId
                }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val theirDisplayName: StateFlow<String?> =
        theirUserId
            .map { theirUserId ->
                theirUserId?.let { userId ->
                    matrixClient.api.user.getProfileField(userId, DisplayName).fold({ it.value }, { null })
                        ?: theirUserId.full
                }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val isFromOwnAccount: StateFlow<Boolean?> =
        theirUserId.map { it == userId }.stateIn(coroutineScope, WhileSubscribed(), null)
    @OptIn(ExperimentalCoroutinesApi::class)
    override val activeVerificationState =
        activeVerification.flatMapLatest { it?.state ?: flowOf(null) }.stateIn(coroutineScope, Eagerly, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sasStep =
        activeVerificationState
            .flatMapLatest { step ->
                if (step is ActiveVerificationState.Start && step.method is ActiveSasVerificationMethod) {
                    (step.method as? ActiveSasVerificationMethod)?.state ?: flowOf(null)
                } else flowOf(null)
            }
            .stateIn(coroutineScope, Eagerly, null)

    private val isDeviceVerification = activeVerification.map { it is ActiveDeviceVerification }

    override val verificationMethods =
        combine(activeVerificationState, isDeviceVerification) { verificationState, isDeviceVerification ->
                if (verificationState is ActiveVerificationState.Ready) {
                    verificationState.methods.map { verificationMethod ->
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
                } else null
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override fun acceptVerificationMethod(verificationMethod: VerificationMethod) {
        coroutineScope.launch {
            val currentVerificationState = activeVerificationState.value
            if (currentVerificationState is ActiveVerificationState.Ready) {
                log.info { "start verification" }
                currentVerificationState.start(verificationMethod)
            } else {
                log.warn {
                    "trying to accept verification method $verificationMethod, but the current verification state is '$currentVerificationState'"
                }
            }
        }
    }

    override val emojis: StateFlow<List<Pair<String, Map<String, String>>>?> =
        sasStep
            .map {
                if (it is ActiveSasVerificationState.ComparisonByUser) {
                    it.emojis.map { (number, _) ->
                        emojisWithTranslation[number]
                            ?: throw IllegalArgumentException("Cannot find emoji for number $number.")
                    }
                } else {
                    null
                }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    override val decimals: StateFlow<List<Int>?> =
        sasStep
            .map {
                if (it is ActiveSasVerificationState.ComparisonByUser) {
                    it.decimal
                } else {
                    null
                }
            }
            .stateIn(coroutineScope, WhileSubscribed(), null)

    init {
        // Necessary to handle back button presses while in the setup, whose back callback has a higher
        // priority because
        // of the underlying viewModel backHandlers
        //        registerBackCallback(
        BackCallback(priority = BackHandler.PRIORITY_VERIFICATION) {
            if (activeVerificationState.value !is ActiveVerificationState.Cancel) {
                cancel()
            } else {
                onVerificationNotOk()
            }
        }

        coroutineScope.launch {
            if (timelineEventId == null) {
                if (matrixClient.verification.activeDeviceVerification.value == null) {
                    log.warn { "Found no active verification, cancelling verification process" }
                    onCloseVerification()
                }
                matrixClient.verification.activeDeviceVerification.filterNotNull().collectLatest {
                    log.debug { "new device verification" }
                    activeVerification.value = it
                    verificationSteps()
                }
            } else {
                roomId?.let {
                    matrixClient.room.getTimelineEvent(roomId, timelineEventId).collectLatest {
                        it?.let { timelineEvent ->
                            activeVerification.value =
                                matrixClient.verification.getActiveUserVerification(
                                    timelineEvent.roomId,
                                    timelineEvent.eventId,
                                )
                            if (activeVerification.value != null) {
                                log.debug { "new user verification in room $roomId (timelineEvent $timelineEvent)" }
                                verificationSteps()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun acceptEmojisOrNumbers() {
        coroutineScope.launch {
            log.debug { "accept verification" }
            verification { it.match() }
        }
    }

    override fun declineEmojisOrNumbers() {
        coroutineScope.launch {
            log.debug { "decline verification" }
            verification { it.noMatch() }
        }
    }

    private suspend fun verificationSteps() = coroutineScope {
        var verificationJob: Job? = null
        activeVerificationState.filterNotNull().collect { verificationState ->
            log.debug { "active verification step: ${verificationState::class}" }
            when (verificationState) {
                is ActiveVerificationState.Start -> {
                    verificationJob?.cancelAndJoin()
                    verificationJob = coroutineScope.launch {
                        when (val method = verificationState.method) {
                            is ActiveSasVerificationMethod -> {
                                method.state.collect { methodState ->
                                    log.debug { "started verification, method state: $methodState" }
                                    onSasStateChange(methodState)
                                }
                            }
                        }
                    }
                }

                is ActiveVerificationState.Cancel,
                is ActiveVerificationState.WaitForDone,
                is ActiveVerificationState.Done -> {
                    verificationJob?.cancelAndJoin()
                    onStateChange(verificationState)
                }

                else -> onStateChange(verificationState)
            }
        }
    }

    override fun cancel() {
        activeVerification.value?.let { activeVerification ->
            coroutineScope.launch {
                try {
                    activeVerification.cancel()
                } catch (_: Exception) {
                    onCloseVerification()
                }
            }
        }
    }

    override fun acceptRequest() {
        activeVerification.value?.let { activeVerification ->
            val verificationState = activeVerification.state.value
            if (verificationState is ActiveVerificationState.TheirRequest) {
                coroutineScope.launch { verificationState.ready() }
            } else {
                log.warn { "Verification state is not ready (actual state: $verificationState)" }
            }
        }
    }

    override fun ok() {
        when (val activeStep = activeVerificationState.value) {
            is ActiveVerificationState.Done -> onVerificationSuccessOk()
            is ActiveVerificationState.Cancel -> onVerificationNotOk()

            else -> {
                log.warn { "Can't proceed on given step ($activeStep)" }
            }
        }
    }

    private fun onVerificationSuccessOk() {
        onCloseVerification()
    }

    private fun onVerificationNotOk() = coroutineScope.launch {
        val thisDeviceTrustLevel = matrixClient.key.getTrustLevel(matrixClient.userId, matrixClient.deviceId).first()
        if (thisDeviceTrustLevel.isVerified.not()) {
            onRedoSelfVerification()
        }
        onCloseVerification()
    }

    private suspend fun verification(reaction: suspend (ActiveSasVerificationState.ComparisonByUser) -> Unit) {
        sasStep.value?.let { activeSasStep ->
            if (activeSasStep is ActiveSasVerificationState.ComparisonByUser) {
                try {
                    reaction(activeSasStep)
                } catch (_: Exception) {
                    activeVerification.value?.cancel()
                }
            }
        }
    }

    override fun acceptSasVerification() {
        log.debug { "user accepted SAS start" }
        coroutineScope.launch {
            val activeSasStep = sasStep.value
            if (activeSasStep is ActiveSasVerificationState.TheirSasStart) {
                log.info { "user accepted SAS start" }
                activeSasStep.accept()
            } else {
                log.warn { "active SAS verification is not in state 'TheirSasStart' (actual state: $activeSasStep)" }
            }
        }
    }
}
