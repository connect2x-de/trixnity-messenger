package de.connect2x.trixnity.messenger.viewmodel.room

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.util.isKnock
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface JoinRoomActionViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        via: Set<String>?,
        onOpenRoom: (roomId: RoomId) -> Unit,
        onDismiss: () -> Unit,
    ) = JoinRoomActionViewModelImpl(viewModelContext, roomId, via, onOpenRoom, onDismiss)

    companion object : JoinRoomActionViewModelFactory
}

interface JoinRoomActionViewModel {
    val actionNecessary: StateFlow<JoinRoomAction?>
    val error: StateFlow<String?>

    sealed class JoinRoomAction {
        data class Join(val onJoinRoom: () -> Unit, val onDismiss: () -> Unit) : JoinRoomAction()

        data class Knock(val onKnock: () -> Unit, val onDismiss: () -> Unit) : JoinRoomAction()

        data class Restricted(val requiredRooms: Set<RoomId>, val onDismiss: () -> Unit) : JoinRoomAction()

        data class AcceptInvitation(val onAcceptInvite: () -> Unit, val onDismiss: () -> Unit) : JoinRoomAction()

        data class Private(val onDismiss: () -> Unit) : JoinRoomAction()

        data class NotFound(val onDismiss: () -> Unit) : JoinRoomAction()
    }
}

class JoinRoomActionViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val via: Set<String>?,
    private val onOpenRoom: (roomId: RoomId) -> Unit,
    private val onDismiss: () -> Unit,
) : JoinRoomActionViewModel, MatrixClientViewModelContext by viewModelContext {

    init {
        val backCallback = BackCallback(onBack = onDismiss)
        trixnityMessengerBackHandler.registerBackCallback(backCallback)
    }

    override val actionNecessary: StateFlow<JoinRoomActionViewModel.JoinRoomAction?> =
        combine(
                matrixClient.room.getById(roomId).map { it?.membership },
                matrixClient.room.getState(roomId, JoinRulesEventContent::class).map {
                    // if room isn't locally accessible, request summary from server
                    if (it?.content != null) {
                        it.content.joinRule to
                            it.content.allow
                                ?.filter {
                                    it.type == JoinRulesEventContent.AllowCondition.AllowConditionType.RoomMembership
                                }
                                ?.map { it.roomId }
                                ?.toSet()
                    } else {
                        matrixClient.api.room.getSummary(roomId).getOrNull().let { it?.joinRule to it?.allowedRoomIds }
                    }
                },
            ) { membership, joinRule ->
                return@combine when {
                    membership == Membership.JOIN -> {
                        log.warn {
                            "Already joined room $roomId, no confirmation necessary, returning null and opening room"
                        }
                        onOpenRoom(roomId)
                        null
                    }

                    membership == Membership.INVITE -> {
                        log.debug { "Got an invitation for room $roomId, showing option to accept" }
                        JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation(::onConfirmJoin, onDismiss)
                    }

                    joinRule.first == JoinRulesEventContent.JoinRule.Public -> {
                        log.debug { "Room $roomId is public, showing option to join" }
                        JoinRoomActionViewModel.JoinRoomAction.Join(::onConfirmJoin, onDismiss)
                    }

                    joinRule.first?.isKnock ?: false -> {
                        log.debug { "Room $roomId is knock, showing option to knock" }
                        JoinRoomActionViewModel.JoinRoomAction.Knock(::onConfirmKnock, onDismiss)
                    }

                    // Only show restricted action when there are room join conditions
                    joinRule.first == JoinRulesEventContent.JoinRule.Restricted -> {
                        val allowConditionsRooms = joinRule.second
                        if (allowConditionsRooms?.isNotEmpty() ?: false) {
                            log.debug {
                                "Room $roomId is restricted, showing rooms $allowConditionsRooms as precondition"
                            }
                            JoinRoomActionViewModel.JoinRoomAction.Restricted(allowConditionsRooms, onDismiss)
                        } else {
                            log.debug {
                                "Room $roomId is restricted, but there are no rooms as conditions, showing private action"
                            }
                            JoinRoomActionViewModel.JoinRoomAction.Private(onDismiss)
                        }
                    }

                    joinRule.first in
                        setOf(JoinRulesEventContent.JoinRule.Private, JoinRulesEventContent.JoinRule.Invite) -> {
                        log.debug {
                            "No action to join room $roomId with join rule ${joinRule.first} available, returning private action"
                        }
                        JoinRoomActionViewModel.JoinRoomAction.Private(onDismiss)
                    }

                    else -> {
                        log.debug { "Room $roomId couldn't be found, showing not found action" }
                        JoinRoomActionViewModel.JoinRoomAction.NotFound(onDismiss)
                    }
                }
            }
            .stateIn(coroutineScope, Eagerly, null)

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private fun onConfirmJoin() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                log.debug { "try to join room while not connected" }
                _error.value = i18n.joinRoomConfirmJoinOffline()
            } else {
                log.debug { "try to join room $roomId via $via" }
                matrixClient.api.room
                    .joinRoom(roomId, via)
                    .fold(
                        onSuccess = { onOpenRoom(it) },
                        onFailure = {
                            log.error(it) { "Cannot join room." }
                            _error.value = i18n.roomListInvitationError()
                        },
                    )
            }
        }
    }

    private fun onConfirmKnock() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                log.debug { "try to knock on room while not connected" }
                _error.value = i18n.joinRoomConfirmJoinOffline()
            } else {
                log.debug { "try to knock on room $roomId" }
                matrixClient.api.room
                    .knockRoom(roomId)
                    .fold(
                        onSuccess = { onOpenRoom(it) },
                        onFailure = {
                            log.error(it) { "Cannot knock on room." }
                            _error.value = i18n.roomListInvitationError()
                        },
                    )
            }
        }
    }
}
