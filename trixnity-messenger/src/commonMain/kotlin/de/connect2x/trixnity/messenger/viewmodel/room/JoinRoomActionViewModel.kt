package de.connect2x.trixnity.messenger.viewmodel.room

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.clientserverapi.client.SyncState
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.core.model.events.m.room.Membership
import de.connect2x.trixnity.messenger.util.isKnock
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
        onOpenRoom: (roomId: RoomId) -> Unit,
        onDismiss: () -> Unit
    ) = JoinRoomActionViewModelImpl(viewModelContext, roomId, onOpenRoom, onDismiss)

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
        data class Impossible(val onDismiss: () -> Unit) : JoinRoomAction()
    }
}

class JoinRoomActionViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val roomId: RoomId,
    private val onOpenRoom: (roomId: RoomId) -> Unit,
    private val onDismiss: () -> Unit
) :
    JoinRoomActionViewModel, MatrixClientViewModelContext by viewModelContext {
    override val actionNecessary: StateFlow<JoinRoomActionViewModel.JoinRoomAction?> =
        combine(
            matrixClient.room.getById(roomId).map { it?.membership },
            matrixClient.room.getState(roomId, JoinRulesEventContent::class).map {
                it?.content
            }
        ) { membership, joinRuleContent ->
            return@combine when {
                membership == Membership.JOIN -> {
                    log.warn { "Already joined room $roomId, no confirmation necessary, returning null" }
                    null
                }

                membership == Membership.INVITE -> {
                    JoinRoomActionViewModel.JoinRoomAction.AcceptInvitation(::onAcceptInvite, onDismiss)
                }

                joinRuleContent?.joinRule == JoinRulesEventContent.JoinRule.Public -> JoinRoomActionViewModel.JoinRoomAction.Join(
                    ::onConfirmJoin, onDismiss
                )

                joinRuleContent?.joinRule?.isKnock
                    ?: false -> JoinRoomActionViewModel.JoinRoomAction.Knock(::onConfirmKnock, onDismiss)

                //Only show restricted action when there are room join conditions
                joinRuleContent?.joinRule == JoinRulesEventContent.JoinRule.Restricted -> {
                    val allowConditionsRooms =
                        joinRuleContent.allow?.filter { it.type == JoinRulesEventContent.AllowCondition.AllowConditionType.RoomMembership }
                            ?.map { it.roomId }?.toSet()
                    if (allowConditionsRooms?.isNotEmpty() ?: false) {
                        JoinRoomActionViewModel.JoinRoomAction.Restricted(allowConditionsRooms, onDismiss)
                    } else {
                        JoinRoomActionViewModel.JoinRoomAction.Impossible(onDismiss)
                    }
                }

                else -> JoinRoomActionViewModel.JoinRoomAction.Impossible(onDismiss)
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()
    private fun onConfirmJoin() {
        coroutineScope.launch {
            if (matrixClient.syncState.value == SyncState.ERROR) {
                log.debug { "try to join room while not connected" }
                _error.value = i18n.joinRoomConfirmJoinOffline()
            } else {
                log.debug { "try to join room $roomId" }
                matrixClient.api.room.joinRoom(roomId).fold(
                    onSuccess = {
                        onOpenRoom(it)
                    },
                    onFailure = {
                        log.error(it) { "Cannot join room." }
                        _error.value = i18n.roomListInvitationError()
                    }
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
                matrixClient.api.room.knockRoom(roomId).fold(
                    onSuccess = {
                        onOpenRoom(it)
                    },
                    onFailure = {
                        log.error(it) { "Cannot knock on room." }
                        _error.value = i18n.roomListInvitationError()
                    }
                )
            }
        }
    }

    private fun onAcceptInvite() {}


}
