package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import de.connect2x.trixnity.messenger.Parcel
import de.connect2x.trixnity.messenger.Parceler
import de.connect2x.trixnity.messenger.RawValue
import de.connect2x.trixnity.messenger.getClassLoader
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepConfig
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepConfig.*
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel.VerificationStepWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        object None : VerificationStepWrapper()
        object Wait : VerificationStepWrapper()

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

        object AcceptedByOtherClient : VerificationStepWrapper()
    }

    sealed class VerificationStepConfig : Parcelable {

        @Parcelize
        object None : VerificationStepConfig()

        @Parcelize
        object Wait : VerificationStepConfig()

        @Parcelize
        data class Request(val theirUserId: @RawValue UserId?, val fromDeviceId: String) : VerificationStepConfig()

        @Parcelize
        data class SelectVerificationMethod(
            val verificationMethods: Set<VerificationMethod>,
            val roomId: RoomId?,
            val timelineEventId: EventId?,
            val isDeviceVerification: Boolean,
        ) : VerificationStepConfig() {

            private companion object : Parceler<SelectVerificationMethod> {

                override fun SelectVerificationMethod.write(parcel: Parcel, flags: Int) {
                    parcel.writeArray(verificationMethods.toTypedArray())
                    parcel.writeString(roomId?.full)
                    parcel.writeString(timelineEventId?.full)
                    parcel.writeBoolean(isDeviceVerification)
                }

                override fun create(parcel: Parcel): SelectVerificationMethod {
                    val verificationMethods = parcel.readArray(getClassLoader())?.toSet() as Set<VerificationMethod>
                    val roomId = parcel.readString()?.let { RoomId(it) }
                    val timelineEventId = parcel.readString()?.let { EventId(it) }
                    val isDeviceVerification = parcel.readBoolean()
                    return SelectVerificationMethod(verificationMethods, roomId, timelineEventId, isDeviceVerification)
                }
            }
        }

        @Parcelize
        data class AcceptSasStart(
            val roomId: RoomId?,
            val timelineEventId: EventId?,
        ) : VerificationStepConfig() {

            private companion object : Parceler<AcceptSasStart> {

                override fun AcceptSasStart.write(parcel: Parcel, flags: Int) {
                    parcel.writeString(roomId?.full)
                    parcel.writeString(timelineEventId?.full)
                }

                override fun create(parcel: Parcel): AcceptSasStart {
                    val roomId = parcel.readString()?.let { RoomId(it) }
                    val timelineEventId = parcel.readString()?.let { EventId(it) }
                    return AcceptSasStart(roomId, timelineEventId)
                }
            }
        }

        @Parcelize
        data class CompareEmojisOrNumbers(val decimals: List<Int>, val emojis: List<Pair<Int, String>>) :
            VerificationStepConfig()

        @Parcelize
        data class Success(val fromDeviceId: String?) : VerificationStepConfig()

        @Parcelize
        object Rejected : VerificationStepConfig()

        @Parcelize
        object Timeout : VerificationStepConfig()

        @Parcelize
        object Cancelled : VerificationStepConfig()

        @Parcelize
        object AcceptedByOtherClient : VerificationStepConfig()
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

                    is ActiveVerificationState.PartlyDone -> {
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

                    else -> {
                        log.warn { "unknown verification state: $verificationState" }
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
