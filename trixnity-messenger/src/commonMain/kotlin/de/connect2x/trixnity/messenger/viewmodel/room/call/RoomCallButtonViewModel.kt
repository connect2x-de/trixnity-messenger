package de.connect2x.trixnity.messenger.viewmodel.room.call

import de.connect2x.trixnity.client.rtc
import de.connect2x.trixnity.client.rtc.RtcSlotMembership
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.core.MSC4193
import de.connect2x.trixnity.core.MSC4195
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.rtc.CallRtcApplication
import de.connect2x.trixnity.core.model.events.m.rtc.LiveKitRtcTransport
import de.connect2x.trixnity.core.model.events.m.rtc.RtcApplicationMember
import de.connect2x.trixnity.core.model.events.m.rtc.RtcMemberEventContent
import de.connect2x.trixnity.core.model.events.m.rtc.RtcSlotEventContent
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallMediaFactory
import de.connect2x.trixnity.messenger.call.CallService
import de.connect2x.trixnity.messenger.call.CallSessionFactory
import de.connect2x.trixnity.messenger.call.CallTarget
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.restoreCall
import de.connect2x.trixnity.messenger.util.getOrNull
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get
import org.koin.core.component.inject

@TrixnityMessengerCallApi
interface RoomCallButtonViewModelFactory {
    fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): RoomCallButtonViewModel?
}

@TrixnityMessengerCallApi
class RoomCallButtonViewModelFactoryImpl(private val messengerConfiguration: MatrixMessengerConfiguration) :
    RoomCallButtonViewModelFactory {
    override fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId): RoomCallButtonViewModel? {
        if (!messengerConfiguration.features.enableVideoCalls) {
            return null
        }

        return RoomCallButtonViewModelImpl(
            viewModelContext = viewModelContext,
            roomId = roomId,
            calls = viewModelContext.getOrNull<CallService>(),
            sessionFactory =
                if (viewModelContext.getOrNull<CallMediaFactory>() != null)
                    viewModelContext.getOrNull<CallSessionFactory>()
                else null,
        )
    }
}

@TrixnityMessengerCallApi
interface RoomCallButtonViewModel {
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
class RoomCallButtonViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val calls: CallService?,
    private val sessionFactory: CallSessionFactory?,
) : RoomCallButtonViewModel, MatrixClientViewModelContext by viewModelContext {

    init {
        check(get<MatrixMessengerConfiguration>().features.enableVideoCalls) {
            "RoomCallButtonViewModel must not be used without video calls enabled"
        }
    }

    private val slotId = CallRtcApplication.SLOT_ID
    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val routeNavigation = getOrNull<RouteNavigation>()
    private val membership =
        matrixClient.rtc.getCurrentMemberships().map { it.find { it.roomId == roomId && it.slotId == slotId } }
    private val hasParticipants =
        matrixClient.rtc.getMembers(roomId, slotId).map { events ->
            events.values.any { it.content is RtcMemberEventContent.Join }
        }

    private val canCreateOrJoin =
        combine(
            matrixClient.serverData.map { it?.rtcTransports },
            matrixClient.rtc.getSlot(roomId, slotId).map { it?.content is RtcSlotEventContent.Open },
            matrixClient.user.canSendEvent<RtcSlotEventContent>(roomId),
            matrixClient.user.canSendEvent<RtcMemberEventContent>(roomId),
        ) { transports, isCallSlotPresent, canSendSlotEvent, canSendMemberEvent ->
            when {
                transports.orEmpty().none { it is LiveKitRtcTransport } -> false // TODO use foreign SFU when present
                isCallSlotPresent -> canSendMemberEvent
                else -> canSendSlotEvent && canSendMemberEvent
            }
        }

    private val configuration: MatrixMessengerConfiguration by inject()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val state: StateFlow<RoomCallButtonViewModel.State?> = let {
        if (!configuration.features.enableVideoCalls) return@let MutableStateFlow(RoomCallButtonViewModel.State.NoCall)
        // TODO(fhilgers): can we check that features are only enabled when nav3 is enabled?
        if (routeNavigation == null) return@let MutableStateFlow(RoomCallButtonViewModel.State.NoCall)

        messengerSettings
            .map { it.experimental.enableVideoCalls }
            .distinctUntilChanged()
            .flatMapLatest { enabled ->
                if (!enabled || calls == null || sessionFactory == null)
                    return@flatMapLatest flowOf(RoomCallButtonViewModel.State.NoCall)

                combine(
                    membership.flatMapLatest { it?.state ?: flowOf(null) },
                    canCreateOrJoin,
                    hasParticipants,
                    calls.session,
                ) { existingMembershipState, canCreateOrJoin, hasParticipants, activeSession ->
                    when (existingMembershipState) {
                        is RtcSlotMembership.State.Join -> RoomCallButtonViewModel.State.Join
                        is RtcSlotMembership.State.Joining -> RoomCallButtonViewModel.State.Joining
                        is RtcSlotMembership.State.Leaving -> RoomCallButtonViewModel.State.Leaving
                        is RtcSlotMembership.State.JoiningError,
                        is RtcSlotMembership.State.LeavingError ->
                            RoomCallButtonViewModel.State.Error(i18n.callJoinError())

                        is RtcSlotMembership.State.Leave,
                        null ->
                            if (canCreateOrJoin && activeSession == null)
                                RoomCallButtonViewModel.State.CallAvailable(hasParticipants)
                            else RoomCallButtonViewModel.State.NoCall
                    }
                }
            }
            .stateIn(coroutineScope, Eagerly, null)
    }

    override fun join() {
        val service = calls ?: return
        val navigation = routeNavigation ?: return
        if (state.value !is RoomCallButtonViewModel.State.CallAvailable) return
        val factory = sessionFactory ?: return
        val session = factory.create(CallTarget(userId, roomId))
        if (!service.register(session)) {
            session.close()
            return
        }
        navigation.updateNavigation { if (service.session.value === session) restoreCall(session.target) }
    }

    override fun leave() {
        val current = calls?.session?.value
        if (current != null && current.target == CallTarget(userId, roomId)) {
            calls.unregister(current)
        } else
            coroutineScope.launch {
                membership
                    .first()
                    ?.takeIf { it.state.first().content is RtcMemberEventContent.Join }
                    ?.leave(RtcMemberEventContent.Leave.Reason(RtcApplicationMember.DefaultLeaveReasonCode.LEAVE.value))
            }
    }
}

@TrixnityMessengerCallApi
class PreviewRoomCallButtonViewModel : RoomCallButtonViewModel {
    override val state: StateFlow<RoomCallButtonViewModel.State> =
        MutableStateFlow(RoomCallButtonViewModel.State.NoCall)

    override fun join() = Unit

    override fun leave() = Unit
}
