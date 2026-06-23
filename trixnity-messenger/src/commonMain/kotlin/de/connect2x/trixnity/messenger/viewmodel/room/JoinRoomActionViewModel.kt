package de.connect2x.trixnity.messenger.viewmodel.room

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomAliasId
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel.JoinRoomAction.Join
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel.JoinRoomAction.Knock
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel.JoinRoomAction.NotFound
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel.JoinRoomAction.Private
import de.connect2x.trixnity.messenger.viewmodel.room.JoinRoomActionViewModel.JoinRoomAction.Restricted
import de.connect2x.trixnity.messenger.viewmodel.util.hasConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val onDismiss: () -> Unit
    val isLoading: StateFlow<Boolean>

    sealed interface JoinRoomAction {
        data class Join(val onJoinRoom: () -> Unit) : JoinRoomAction

        data class Knock(val onKnock: () -> Unit, val hasKnocked: StateFlow<Boolean?>) : JoinRoomAction

        data class Restricted(val requiredRooms: Set<RoomAliasId>) : JoinRoomAction

        data class AcceptInvitation(val onAcceptInvite: () -> Unit) : JoinRoomAction

        data object Private : JoinRoomAction

        data object NotFound : JoinRoomAction
    }
}

class JoinRoomActionViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val via: Set<String>?,
    private val onOpenRoom: (roomId: RoomId) -> Unit,
    override val onDismiss: () -> Unit,
) : JoinRoomActionViewModel, MatrixClientViewModelContext by viewModelContext {

    init {
        val backCallback = BackCallback(onBack = onDismiss)
        trixnityMessengerBackHandler.registerBackCallback(backCallback)
    }

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val hasKnocked =
        matrixClient.room
            .getById(roomId)
            .map { it?.membership == Membership.KNOCK }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

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
                        matrixClient.api.room.getSummary(roomId, via).getOrNull()?.let {
                            it.joinRule to it.allowedRoomIds
                        }
                    }
                },
            ) { membership, joinRule ->
                return@combine when (membership) {
                    Membership.JOIN -> {
                        log.warn {
                            "Already joined room $roomId, no confirmation necessary, returning null and opening room"
                        }
                        onOpenRoom(roomId)
                        null
                    }

                    Membership.INVITE -> {
                        log.debug { "Got an invitation for room $roomId, showing option to accept" }
                        AcceptInvitation(::onConfirmJoin)
                    }

                    else -> {
                        when (joinRule?.first) {
                            JoinRulesEventContent.JoinRule.Public -> {
                                log.debug { "Room $roomId is public, showing option to join" }
                                Join(::onConfirmJoin)
                            }

                            JoinRulesEventContent.JoinRule.Knock,
                            JoinRulesEventContent.JoinRule.KnockRestricted -> {
                                log.debug { "Room $roomId is knock, showing option to knock" }
                                Knock(::onConfirmKnock, hasKnocked)
                            }

                            // Only show restricted action when there are room join conditions
                            JoinRulesEventContent.JoinRule.Restricted -> {
                                val allowConditionsRooms = joinRule.second
                                if (allowConditionsRooms?.isNotEmpty() ?: false) {
                                    val allowConditionRoomsAliases =
                                        allowConditionsRooms
                                            .mapNotNull {
                                                matrixClient.api.room.getSummary(it).getOrNull()?.canonicalAlias
                                            }
                                            .toSet()
                                    log.debug {
                                        "Room $roomId is restricted, showing rooms $allowConditionRoomsAliases as precondition"
                                    }
                                    Restricted(allowConditionRoomsAliases)
                                } else {
                                    log.debug {
                                        "Room $roomId is restricted, but there are no rooms as conditions, showing private action"
                                    }
                                    Private
                                }
                            }

                            JoinRulesEventContent.JoinRule.Private,
                            JoinRulesEventContent.JoinRule.Invite -> {
                                log.debug {
                                    "No action to join room $roomId with join rule ${joinRule.first} available, returning private action"
                                }
                                Private
                            }

                            is JoinRulesEventContent.JoinRule.Unknown -> {
                                log.debug { "Join rule of room $roomId is unknown, returning private action" }
                                Private
                            }
                            null -> {
                                log.error {
                                    "Couldn't get room join rule information via local storage or summary, returning not found"
                                }
                                NotFound
                            }
                        }
                    }
                }
            }
            .stateIn(coroutineScope, Eagerly, null)

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private val joinActionMutex = Mutex()

    private fun onConfirmJoin() {
        coroutineScope.launch {
            joinActionMutex.withLock {
                _isLoading.value = true
                if (!matrixClient.syncState.value.hasConnection()) {
                    log.debug { "try to join room while not connected" }
                    _error.value = i18n.joinRoomConfirmJoinOffline()
                } else {
                    log.debug { "try to join room $roomId via $via" }
                    matrixClient.api.room
                        .joinRoom(roomId, via)
                        .fold(
                            onSuccess = { _error.value = null },
                            onFailure = {
                                log.error(it) { "Cannot join room." }
                                _error.value = i18n.roomListInvitationError()
                            },
                        )
                }
                _isLoading.value = false
            }
        }
    }

    private fun onConfirmKnock() {
        coroutineScope.launch {
            joinActionMutex.withLock {
                _isLoading.value = true
                if (!matrixClient.syncState.value.hasConnection()) {
                    log.debug { "try to knock on room while not connected" }
                    _error.value = i18n.joinRoomConfirmJoinOffline()
                } else {
                    log.debug { "try to knock on room $roomId" }
                    matrixClient.api.room
                        .knockRoom(roomId, via)
                        .fold(
                            onSuccess = { _error.value = null },
                            onFailure = {
                                log.error(it) { "Cannot knock on room." }
                                _error.value = i18n.roomListInvitationError()
                            },
                        )
                }
                _isLoading.value = false
            }
        }
    }
}
