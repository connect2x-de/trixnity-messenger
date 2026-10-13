package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.client.rtc
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.toUserInfoElement
import de.connect2x.trixnity.messenger.viewmodel.util.Initials
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.inject

@TrixnityMessengerCallApi
interface CallViewModelFactory {
    fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): CallViewModel?
}

@TrixnityMessengerCallApi
class CallViewModelFactoryImpl(private val messengerConfiguration: MatrixMessengerConfiguration) :
    CallViewModelFactory {
    override fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): CallViewModel? {
        if (!messengerConfiguration.features.enableVideoCalls) {
            return null
        }

        return CallViewModelImpl(viewModelContext, roomId)
    }
}

@TrixnityMessengerCallApi
interface CallViewModel {
    val state: StateFlow<State>
    val canJoinCall: StateFlow<Boolean>

    fun join()

    fun leave()

    sealed interface State {
        data object Inactive : State

        data object Active : State

        data object Joined : State

        data class Error(val error: String) : State
    }
}

@OptIn(MSC4143::class)
@TrixnityMessengerCallApi
class CallViewModelImpl(viewModelContext: MatrixClientViewModelContext, roomId: RoomId) :
    CallViewModel, MatrixClientViewModelContext by viewModelContext {
    private val initials: Initials by inject()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val participants: StateFlow<List<UserInfoElement>> =
        matrixClient.rtc
            .getMembers(roomId, CallRtcApplication.SLOT_ID)
            .flatMapLatest { events ->
                if (events.isEmpty()) return@flatMapLatest flowOf(emptyList())
                combine(events.map { (sender, event) -> matrixClient.user.getById(roomId, sender.first) }) { users ->
                    users.mapNotNull {
                        it?.toUserInfoElement(coroutineScope, matrixClient, initials, avatarSize().toLong())
                    }
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())
    override val state: StateFlow<CallViewModel.State> =
        participants
            .map { if (it.isEmpty()) CallViewModel.State.Inactive else CallViewModel.State.Active }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), CallViewModel.State.Inactive)
    override val canJoinCall: StateFlow<Boolean> =
        combine(
                matrixClient.serverData.map { it?.rtcTransports },
                matrixClient.rtc.getSlot(roomId, CallRtcApplication.SLOT_ID).map { it != null },
                matrixClient.user.canSendEvent<RtcSlotEventContent>(roomId),
                matrixClient.user.canSendEvent<RtcMemberEventContent>(roomId),
            ) { transports, isCallSlotPresent, canSendSlotEvent, canSendMemberEvent ->
                if (transports == null) return@combine false

                when (isCallSlotPresent) {
                    true -> canSendMemberEvent
                    false -> canSendSlotEvent && canSendMemberEvent
                }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    override fun join() = Unit

    override fun leave() = Unit
}

@TrixnityMessengerCallApi
class PreviewCallViewModel : CallViewModel {
    override val state: StateFlow<CallViewModel.State> = MutableStateFlow(CallViewModel.State.Inactive)
    override val canJoinCall: StateFlow<Boolean> = MutableStateFlow(true)

    override fun join() = Unit

    override fun leave() = Unit
}
