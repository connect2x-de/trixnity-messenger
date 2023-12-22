package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepConfig
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepConfig.*
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.verification.*
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface VerificationViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onCloseVerification: () -> Unit,
        onRedoSelfVerification: () -> Unit,
        roomId: RoomId?,
        timelineEventId: EventId?,
    ): VerificationViewModel {
        return VerificationViewModelImpl(
            viewModelContext, onCloseVerification, onRedoSelfVerification, roomId, timelineEventId,
        )
    }

    companion object : VerificationViewModelFactory
}

interface VerificationViewModel {
    val stack: Value<ChildStack<VerificationStepConfig, VerificationStepWrapper>>
    fun cancel()

    sealed class VerificationStepWrapper {
        data object None : VerificationStepWrapper()
        data object Wait : VerificationStepWrapper()

        class Request(val verificationStepRequestViewModel: VerificationStepRequestViewModel) :
            VerificationStepWrapper()

        class SelectVerificationMethod(val selectVerificationMethodViewModel: SelectVerificationMethodViewModel) :
            VerificationStepWrapper()

        class AcceptSasStart(val acceptSasStartViewModel: AcceptSasStartViewModel) : VerificationStepWrapper()

        class CompareEmojisOrNumbers(val verificationStepCompareViewModel: VerificationStepCompareViewModel) :
            VerificationStepWrapper()

        class Success(val verificationStepSuccessViewModel: VerificationStepSuccessViewModel) :
            VerificationStepWrapper()

        class Rejected(val verificationStepRejectedViewModel: VerificationStepRejectedViewModel) :
            VerificationStepWrapper()

        class Timeout(val verificationStepTimeoutViewModel: VerificationStepTimeoutViewModel) :
            VerificationStepWrapper()

        class Cancelled(val verificationStepCancelledViewModel: VerificationStepCancelledViewModel) :
            VerificationStepWrapper()

        data object AcceptedByOtherClient : VerificationStepWrapper()
    }

    @Serializable
    sealed class VerificationStepConfig {

        @Serializable
        data object None : VerificationStepConfig()

        @Serializable
        data object Wait : VerificationStepConfig()

        @Serializable
        data class Request(val theirUserId: UserId?, val fromDeviceId: String) : VerificationStepConfig()

        @Serializable
        data class SelectVerificationMethod(
            val verificationMethods: Set<VerificationMethod>,
            val roomId: RoomId?,
            val timelineEventId: EventId?,
            val isDeviceVerification: Boolean,
        ) : VerificationStepConfig()

        @Serializable
        data class AcceptSasStart(
            val roomId: RoomId?,
            val timelineEventId: EventId?,
        ) : VerificationStepConfig()

        @Serializable
        data class CompareEmojisOrNumbers(val decimals: List<Int>, val emojis: List<Pair<Int, String>>) :
            VerificationStepConfig()

        @Serializable
        data class Success(val fromDeviceId: String?) : VerificationStepConfig()

        @Serializable
        data object Rejected : VerificationStepConfig()

        @Serializable
        data object Timeout : VerificationStepConfig()

        @Serializable
        data object Cancelled : VerificationStepConfig()

        @Serializable
        data object AcceptedByOtherClient : VerificationStepConfig()
    }
}

open class VerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseVerification: () -> Unit,
    private val onRedoSelfVerification: () -> Unit,
    private val roomId: RoomId?,
    private val timelineEventId: EventId?,
) : MatrixClientViewModelContext by viewModelContext, VerificationViewModel {

    private val getActiveVerification = get<GetActiveVerification>()

    private val activeVerification = MutableStateFlow<ActiveVerification?>(null)

    private val navigation = StackNavigation<VerificationStepConfig>()
    override val stack = childStack(
        source = navigation,
        serializer = VerificationStepConfig.serializer(),
        initialConfiguration = None,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(
        config: VerificationStepConfig,
        componentContext: ComponentContext
    ): VerificationStepWrapper =
        when (config) {
            is None -> VerificationStepWrapper.None
            is Request -> VerificationStepWrapper.Request(
                get<VerificationStepRequestViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onRequestAccept = ::onRequestAccept,
                        theirUserId = config.theirUserId,
                        fromDeviceId = config.fromDeviceId,
                    )
            )

            is Wait -> VerificationStepWrapper.Wait
            is SelectVerificationMethod -> VerificationStepWrapper.SelectVerificationMethod(
                get<SelectVerificationMethodViewModelFactory>().create(
                    viewModelContext = childContext(componentContext),
                    verificationMethods = config.verificationMethods,
                    roomId = config.roomId,
                    timelineEventId = config.timelineEventId,
                    isDeviceVerification = config.isDeviceVerification,
                )
            )

            is AcceptSasStart -> VerificationStepWrapper.AcceptSasStart(
                get<AcceptSasStartViewModelFactory>().create(
                    viewModelContext = childContext(componentContext),
                    roomId = config.roomId,
                    timelineEventId = config.timelineEventId,
                )
            )

            is CompareEmojisOrNumbers -> VerificationStepWrapper.CompareEmojisOrNumbers(
                get<VerificationStepCompareViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        decimals = config.decimals,
                        emojisWithoutTranslation = config.emojis,
                        onAccept = ::onAcceptVerification,
                        onDecline = ::onDeclineVerification,
                    )
            )

            is Success -> VerificationStepWrapper.Success(
                get<VerificationStepSuccessViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        fromDeviceId = config.fromDeviceId,
                        onVerificationSuccessOk = ::onVerificationSuccessOk,
                    )
            )

            is Rejected -> VerificationStepWrapper.Rejected(
                get<VerificationStepRejectedViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationRejectedOk = ::onVerificationNotOk,
                    )
            )

            is Timeout -> VerificationStepWrapper.Timeout(
                get<VerificationStepTimeoutViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationTimeoutOk = ::onVerificationNotOk,
                    )
            )

            is Cancelled -> VerificationStepWrapper.Cancelled(
                get<VerificationStepCancelledViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationCancelledOk = ::onVerificationNotOk,
                    )
            )

            is AcceptedByOtherClient -> VerificationStepWrapper.AcceptedByOtherClient
        }


    init {
        coroutineScope.launch {
            if (timelineEventId == null) {
                getActiveVerification.activeDeviceVerification(matrixClient)
                    .filterNotNull()
                    .collectLatest {
                        log.debug { "new device verification" }
                        activeVerification.value = it
                        verificationSteps()
                    }
            } else {
                roomId?.let {
                    matrixClient.room.getTimelineEvent(roomId, timelineEventId).collectLatest {
                        it?.let { timelineEvent ->
                            activeVerification.value =
                                getActiveVerification.activeUserVerification(matrixClient, timelineEvent)
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

    private suspend fun verificationSteps() = coroutineScope {
        activeVerification.value?.let { activeVerification ->
            var verificationJob: Job? = null
            activeVerification.state.collect { verificationState ->
                log.debug { "active verification step: ${verificationState::class}" }
                when (verificationState) {
                    is ActiveVerificationState.OwnRequest -> {
                        navigation.replaceCurrentSuspending(Wait)
                    }

                    is ActiveVerificationState.TheirRequest -> {
                        navigation.replaceCurrentSuspending(
                            Request(
                                activeVerification.theirUserId,
                                verificationState.content.fromDevice
                            )
                        )
                    }

                    is ActiveVerificationState.Ready -> {
                        navigation.replaceCurrentSuspending(
                            SelectVerificationMethod(
                                verificationState.methods,
                                roomId,
                                timelineEventId,
                                isDeviceVerification = activeVerification is ActiveDeviceVerification,
                            )
                        )
                    }

                    is ActiveVerificationState.AcceptedByOtherDevice -> {
                        navigation.replaceCurrentSuspending(AcceptedByOtherClient)
                    }

                    is ActiveVerificationState.Start -> {
                        verificationJob?.cancelAndJoin()
                        verificationJob = launch {
                            when (val method = verificationState.method) {
                                is ActiveSasVerificationMethod -> {
                                    method.state.collect { methodState ->
                                        log.debug { "started verification, method state: $methodState" }
                                        when (methodState) {
                                            is ActiveSasVerificationState.OwnSasStart,
                                            is ActiveSasVerificationState.Accept,
                                            is ActiveSasVerificationState.WaitForKeys,
                                            is ActiveSasVerificationState.WaitForMacs -> {
                                                navigation.replaceCurrentSuspending(Wait)
                                            }

                                            is ActiveSasVerificationState.TheirSasStart -> {
                                                navigation.replaceCurrentSuspending(
                                                    AcceptSasStart(roomId, timelineEventId)
                                                )
                                            }

                                            is ActiveSasVerificationState.ComparisonByUser -> {
                                                navigation.replaceCurrentSuspending(
                                                    CompareEmojisOrNumbers(
                                                        methodState.decimal,
                                                        methodState.emojis,
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is ActiveVerificationState.WaitForDone -> {
                        verificationJob?.cancel()
                        navigation.replaceCurrentSuspending(Wait)
                    }

                    is ActiveVerificationState.Done -> {
                        verificationJob?.cancel()
                        navigation.replaceCurrentSuspending(
                            Success(activeVerification.theirDeviceId)
                        )
                    }

                    is ActiveVerificationState.Cancel -> {
                        verificationJob?.cancel()
                        when (verificationState.content.code) {
                            VerificationCancelEventContent.Code.MismatchedSas ->
                                navigation.replaceCurrentSuspending(Rejected)

                            VerificationCancelEventContent.Code.Timeout ->
                                navigation.replaceCurrentSuspending(Timeout)

                            else ->
                                navigation.replaceCurrentSuspending(Cancelled)
                        }
                    }

                    ActiveVerificationState.Undefined -> {
                        log.warn { "undefined verification state: $verificationState" }
                    }
                }
            }
        }
    }

    override fun cancel() {
        activeVerification.value?.let { activeVerification ->
            coroutineScope.launch {
                try {
                    activeVerification.cancel()
                } catch (exc: Exception) {
                    onCloseVerification()
                }
            }
        }
    }

    private fun onRequestAccept() {
        activeVerification.value?.let { activeVerification ->
            val verificationState = activeVerification.state.value
            if (verificationState is ActiveVerificationState.TheirRequest) {
                coroutineScope.launch { verificationState.ready() }
            }
        }
    }

    internal fun onAcceptVerification() = coroutineScope.launch {
        log.debug { "accept verification" }
        verification { it.match() }
    }

    internal fun onDeclineVerification() = coroutineScope.launch {
        log.debug { "decline verification" }
        verification { it.noMatch() }
    }

    private fun onVerificationSuccessOk() {
        onCloseVerification()
    }

    private fun onVerificationNotOk() = coroutineScope.launch {
        val thisDeviceTrustLevel =
            matrixClient.key.getTrustLevel(matrixClient.userId, matrixClient.deviceId).first()
        if (thisDeviceTrustLevel.isVerified.not()) {
            onRedoSelfVerification()
        }
        onCloseVerification()
    }

    private suspend fun verification(reaction: suspend (ActiveSasVerificationState.ComparisonByUser) -> Unit) {
        activeVerification.value?.let { activeVerification ->
            val verificationState = activeVerification.state.value
            if (verificationState is ActiveVerificationState.Start) {
                val method = verificationState.method
                if (method is ActiveSasVerificationMethod) {
                    val methodState = method.state.value
                    if (methodState is ActiveSasVerificationState.ComparisonByUser) {
                        try {
                            reaction(methodState)
                        } catch (exc: Exception) {
                            activeVerification.cancel()
                        }
                    }
                }
            }
        }
    }
}
