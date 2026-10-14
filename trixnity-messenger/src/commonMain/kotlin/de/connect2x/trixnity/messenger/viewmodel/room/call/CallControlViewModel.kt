package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.rtc
import de.connect2x.trixnity.client.rtc.RtcSlotMembership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.MSC4193
import de.connect2x.trixnity.core.MSC4195
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.core.model.events.ClientEvent
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.LiveKitRtcTransport
import de.connect2x.trixnity.core.model.events.m.rtc.PerMemberRtcEncryption
import de.connect2x.trixnity.core.model.events.m.rtc.RtcApplicationMember
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberId
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.get

@TrixnityMessengerCallApi
interface CallControlViewModelFactory {
    fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): CallControlViewModel?
}

@TrixnityMessengerCallApi
class CallControlViewModelFactoryImpl(private val messengerConfiguration: MatrixMessengerConfiguration) :
    CallControlViewModelFactory {
    override fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): CallControlViewModel? {
        if (!messengerConfiguration.features.enableVideoCalls) {
            return null
        }

        return CallControlViewModelImpl(viewModelContext, roomId)
    }
}

@TrixnityMessengerCallApi
interface CallControlViewModel {
    val state: StateFlow<State?>

    fun join()

    fun leave()

    sealed interface State {
        data object NoCall : State

        data class CallAvailable(val hasParticipants: Boolean) : State

        data object Joining : State

        data object Join : State

        data object Leaving : State

        data class Error(val message: String) : State
    }
}

@OptIn(MSC4143::class, MSC4193::class, MSC4195::class)
@TrixnityMessengerCallApi
class CallControlViewModelImpl(viewModelContext: MatrixClientViewModelContext, private val roomId: RoomId) :
    CallControlViewModel, MatrixClientViewModelContext by viewModelContext {

    init {
        check(get<MatrixMessengerConfiguration>().features.enableVideoCalls) {
            "CallControlViewModel must not be used without video calls enabled"
        }
    }

    private val slotId = CallRtcApplication.SLOT_ID
    private val joining = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val joinedParticipants =
        matrixClient.rtc
            .getMembers(roomId, slotId)
            .map { events ->
                @Suppress("UNCHECKED_CAST")
                events.filterValues { it.content is RtcMemberEventContent.Join }
                    as Map<Pair<UserId, RtcMemberId>, ClientEvent.RoomEvent.MessageEvent<RtcMemberEventContent.Join>>
            }
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val canCreateOrJoin =
        combine(
                matrixClient.serverData.map { it?.rtcTransports },
                matrixClient.rtc.getSlot(roomId, slotId).map { it != null },
                matrixClient.user.canSendEvent<RtcSlotEventContent>(roomId),
                matrixClient.user.canSendEvent<RtcMemberEventContent>(roomId),
            ) { transports, isCallSlotPresent, canSendSlotEvent, canSendMemberEvent ->
                when {
                    transports == null -> false // TODO use foreign SFU when present
                    isCallSlotPresent -> canSendMemberEvent
                    else -> canSendSlotEvent && canSendMemberEvent
                }
            }
            .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), replay = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<CallControlViewModel.State?> =
        combine(
                getExistingMembership().flatMapLatest { it?.state ?: flowOf(null) },
                canCreateOrJoin,
                joinedParticipants,
                error,
                joining,
            ) { existingMembershipState, canCreateOrJoin, participants, error, joining ->
                when (existingMembershipState) {
                    is RtcSlotMembership.State.Join -> CallControlViewModel.State.Join
                    is RtcSlotMembership.State.Joining -> CallControlViewModel.State.Joining
                    is RtcSlotMembership.State.JoiningError -> CallControlViewModel.State.Error(i18n.callJoinError())
                    is RtcSlotMembership.State.Leave ->
                        CallControlViewModel.State.CallAvailable(hasParticipants = participants.isNotEmpty())
                    is RtcSlotMembership.State.Leaving -> CallControlViewModel.State.Leaving
                    is RtcSlotMembership.State.LeavingError -> CallControlViewModel.State.Error(i18n.callJoinError())
                    null ->
                        when {
                            error != null -> CallControlViewModel.State.Error(error)
                            joining -> CallControlViewModel.State.Joining
                            canCreateOrJoin ->
                                CallControlViewModel.State.CallAvailable(hasParticipants = participants.isNotEmpty())
                            else -> CallControlViewModel.State.NoCall
                        }
                }
            }
            .stateIn(coroutineScope, Eagerly, null)

    override fun join() {
        if (!joining.compareAndSet(expect = false, update = true)) return
        coroutineScope.launch {
            error.value = null
            if (state.filterNotNull().first() !is CallControlViewModel.State.CallAvailable) {
                log.warn { "cannot join with wrong state" }
                return@launch
            }
            val existingJoinedMembership = getExistingJoinedMembership()
            if (existingJoinedMembership != null) return@launch

            val hasOrCreateSlot = hasOrCreateSlot()
            if (!hasOrCreateSlot) return@launch
            val transport =
                matrixClient.api.rtc
                    .getTransports()
                    .fold(
                        onSuccess = {
                            val liveKitTransport = it.filterIsInstance<LiveKitRtcTransport>().firstOrNull()
                            if (liveKitTransport == null) {
                                log.warn { "server does not have a LiveKitRtcTransport" }
                            }
                            liveKitTransport
                        },
                        onFailure = {
                            if (it is CancellationException) throw it
                            log.warn(it) { "failed to create slot" }
                            error.value = i18n.callJoinError()
                            null
                        },
                    ) ?: return@launch
            matrixClient.rtc
                .joinSlot(
                    roomId = roomId,
                    slotId = slotId,
                    application = CallRtcApplication.Member(),
                    transports =
                        RtcMemberEventContent.Join.RtcTransports(
                            published = listOf(transport),
                            canSubscribe =
                                listOf(
                                    "m.livekit",
                                    "livekit", // TODO remove Element flavor
                                ),
                        ),
                )
                .fold(
                    onSuccess = {},
                    onFailure = {
                        if (it is CancellationException) throw it
                        log.warn(it) { "failed to join slot" }
                        error.value = i18n.callJoinError()
                    },
                )

            joining.value = false
        }
    }

    override fun leave() {
        error.value = null
        coroutineScope.launch {
            getExistingJoinedMembership()
                ?.leave(RtcMemberEventContent.Leave.Reason(RtcApplicationMember.DefaultLeaveReasonCode.LEAVE.value))
        }
    }

    private fun getExistingMembership() =
        matrixClient.rtc.getCurrentMemberships().map { it.find { it.roomId == roomId && it.slotId == slotId } }

    private suspend fun getExistingJoinedMembership() =
        getExistingMembership().first()?.takeIf {
            val state = it.state.first()
            when (state) {
                is RtcSlotMembership.State.Join,
                is RtcSlotMembership.State.Joining,
                is RtcSlotMembership.State.JoiningError -> true
                is RtcSlotMembership.State.Leave,
                is RtcSlotMembership.State.Leaving,
                is RtcSlotMembership.State.LeavingError -> false
            }
        }

    private suspend fun hasOrCreateSlot(): Boolean {
        val slotEvent = matrixClient.rtc.getSlot(roomId, slotId).first()
        val slot = slotEvent?.content as? RtcSlotEventContent.Open
        if (slot != null) {
            if (
                slot.application != CallRtcApplication.Slot ||
                    slot.encryption == null && slot.encryption != null ||
                    slot.encryption != null && slot.encryption !is PerMemberRtcEncryption
            ) {
                log.info { "the room has an incompatible slot" }
            } else return true
        }
        if (!matrixClient.user.canSendEvent<RtcSlotEventContent>(roomId).first()) {
            log.warn { "cannot create or repair slot" }
            return false
        }
        val encryption =
            if (matrixClient.room.getById(roomId).first()?.encrypted == true) PerMemberRtcEncryption else null
        matrixClient.api.room
            .sendStateEvent(
                roomId = roomId,
                eventContent = RtcSlotEventContent.Open(application = CallRtcApplication.Slot, encryption = encryption),
                stateKey = slotId.value,
            )
            .fold(
                onSuccess = {
                    withTimeoutOrNull(5.seconds) { matrixClient.rtc.getSlot(roomId, slotId).first { it != null }?.id }
                        .also {
                            if (it == null) {
                                log.warn { "slot did not exist after waiting" }
                            }
                        }
                },
                onFailure = {
                    if (it is CancellationException) throw it
                    log.warn(it) { "failed to create slot" }
                    error.value = i18n.callJoinError()
                    return false
                },
            )
        return true
    }
}

@TrixnityMessengerCallApi
class PreviewCallControlViewModel : CallControlViewModel {
    override val state: StateFlow<CallControlViewModel.State> = MutableStateFlow(CallControlViewModel.State.NoCall)

    override fun join() = Unit

    override fun leave() = Unit
}
