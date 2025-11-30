package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.AcceptSasStart
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.AcceptedByOtherClient
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.Cancelled
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.CompareEmojisOrNumbers
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.None
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.Rejected
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.Request
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.SelectVerificationMethod
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.Success
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.Timeout
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Config.Wait
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.Wrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
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
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.roomId
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.ActiveDeviceVerification
import net.folivo.trixnity.client.verification.ActiveSasVerificationMethod
import net.folivo.trixnity.client.verification.ActiveSasVerificationState
import net.folivo.trixnity.client.verification.ActiveVerification
import net.folivo.trixnity.client.verification.ActiveVerificationState
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod
import org.koin.core.component.get
import kotlin.jvm.JvmInline


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
    val userId: UserId
    val stack: Value<ChildStack<Config, Wrapper>>
    fun cancel()

    sealed class Wrapper {
        data object None : Wrapper()
        data object Wait : Wrapper()

        class Request(val viewModel: VerificationStepRequestViewModel) : Wrapper()

        class SelectVerificationMethod(val viewModel: SelectVerificationMethodViewModel) : Wrapper()

        class AcceptSasStart(val viewModel: AcceptSasStartViewModel) : Wrapper()

        class CompareEmojisOrNumbers(val viewModel: VerificationStepCompareViewModel) : Wrapper()

        class Success(val viewModel: VerificationStepSuccessViewModel) : Wrapper()

        class Rejected(val viewModel: VerificationStepRejectedViewModel) : Wrapper()

        class Timeout(val viewModel: VerificationStepTimeoutViewModel) : Wrapper()

        class Cancelled(val viewModel: VerificationStepCancelledViewModel) : Wrapper()

        data object AcceptedByOtherClient : Wrapper()
    }

    @Serializable
    sealed class Config {

        @Serializable
        data object None : Config()

        @Serializable
        data object Wait : Config()

        @Serializable
        data class Request(val theirUserId: UserId?, val fromDeviceId: String) : Config()

        @Serializable
        data class SelectVerificationMethod(
            val verificationMethods: Set<VerificationMethod>,
            val roomId: RoomId?,
            val timelineEventId: EventId?,
            val isDeviceVerification: Boolean,
        ) : Config()

        @Serializable
        data class AcceptSasStart(
            val roomId: RoomId?,
            val timelineEventId: EventId?,
        ) : Config()

        @Serializable
        data class CompareEmojisOrNumbers(val decimals: List<Int>, val emojis: List<Pair<Int, String>>) :
            Config()

        @Serializable
        data object Success : Config()

        @Serializable
        data object Rejected : Config()

        @Serializable
        data object Timeout : Config()

        @Serializable
        data object Cancelled : Config()

        @Serializable
        data object AcceptedByOtherClient : Config()
    }
}

@JvmInline
value class VerificationContext(val coroutineScope: CoroutineScope)

open class VerificationViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onCloseVerification: () -> Unit,
    private val onRedoSelfVerification: () -> Unit,
    private val roomId: RoomId?,
    private val timelineEventId: EventId?,
) : MatrixClientViewModelContext by viewModelContext, VerificationViewModel {
    private val verificationContext = VerificationContext(coroutineScope)

    private val activeVerification = MutableStateFlow<ActiveVerification?>(null)

    private val navigation = StackNavigation<Config>()
    override val stack = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = None,
        childFactory = ::createChild
    )

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (config) {
            is None -> Wrapper.None
            is Request -> Wrapper.Request(
                get<VerificationStepRequestViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onRequestAccept = ::onRequestAccept,
                        theirUserId = config.theirUserId,
                        fromDeviceId = config.fromDeviceId,
                    )
            )

            is Wait -> Wrapper.Wait
            is SelectVerificationMethod -> Wrapper.SelectVerificationMethod(
                get<SelectVerificationMethodViewModelFactory>().create(
                    viewModelContext = childContext(componentContext),
                    verificationContext,
                    verificationMethods = config.verificationMethods,
                    roomId = config.roomId,
                    timelineEventId = config.timelineEventId,
                    isDeviceVerification = config.isDeviceVerification,
                )
            )

            is AcceptSasStart -> Wrapper.AcceptSasStart(
                get<AcceptSasStartViewModelFactory>().create(
                    viewModelContext = childContext(componentContext),
                    verificationContext,
                    roomId = config.roomId,
                    timelineEventId = config.timelineEventId,
                )
            )

            is CompareEmojisOrNumbers -> Wrapper.CompareEmojisOrNumbers(
                get<VerificationStepCompareViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        decimals = config.decimals,
                        emojisWithoutTranslation = config.emojis,
                        onAccept = ::onAcceptVerification,
                        onDecline = ::onDeclineVerification,
                    )
            )

            is Success -> Wrapper.Success(
                get<VerificationStepSuccessViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationSuccessOk = ::onVerificationSuccessOk,
                    )
            )

            is Rejected -> Wrapper.Rejected(
                get<VerificationStepRejectedViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationRejectedOk = ::onVerificationNotOk,
                    )
            )

            is Timeout -> Wrapper.Timeout(
                get<VerificationStepTimeoutViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationTimeoutOk = ::onVerificationNotOk,
                    )
            )

            is Cancelled -> Wrapper.Cancelled(
                get<VerificationStepCancelledViewModelFactory>()
                    .create(
                        viewModelContext = childContext(componentContext),
                        onVerificationCancelledOk = ::onVerificationNotOk,
                    )
            )

            is AcceptedByOtherClient -> Wrapper.AcceptedByOtherClient
        }

    init {
        val setupRunning =
            get<MatrixMessengerSettingsHolder>().value.base.accounts.values.any { !it.base.accountSetupFinished }
        //Necessary to handle back button presses while in the setup, whose back callback has a higher priority because of the underlying viewModel backHandlers
        registerBackCallback(BackCallback(priority = if (setupRunning) 1 else 0) {
            if (stack.value.active.configuration != Cancelled) {
                cancel()
            } else {
                onVerificationNotOk()
            }
        })
        coroutineScope.launch {
            if (timelineEventId == null) {
                matrixClient.verification.activeDeviceVerification
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
                                matrixClient.verification.getActiveUserVerification(
                                    timelineEvent.roomId,
                                    timelineEvent.eventId
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
                        verificationJob = coroutineScope.launch {
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
                            Success
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
