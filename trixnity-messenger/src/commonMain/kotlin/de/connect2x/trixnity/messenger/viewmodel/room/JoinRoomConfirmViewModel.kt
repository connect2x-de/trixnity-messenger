package de.connect2x.trixnity.messenger.viewmodel.room

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent
import de.connect2x.trixnity.messenger.util.isKnock
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

interface JoinRoomConfirmViewModelFactory {
    fun create(viewModelContext: MatrixClientViewModelContext, roomId: RoomId) =
        JoinRoomConfirmViewModelImpl(viewModelContext, roomId)

    companion object : JoinRoomConfirmViewModelFactory
}

interface JoinRoomConfirmViewModel {
    val actionNecessary: StateFlow<JoinRoomAction?>

    sealed class JoinRoomAction {
        data object Join : JoinRoomAction()
        data object Knock : JoinRoomAction()
        data class Restricted(val requiredRooms: Set<RoomId>) : JoinRoomAction()
        data object Impossible : JoinRoomAction()
    }
}

class JoinRoomConfirmViewModelImpl(viewModelContext: MatrixClientViewModelContext, roomId: RoomId) :
    JoinRoomConfirmViewModel, MatrixClientViewModelContext by viewModelContext {
    override val actionNecessary: StateFlow<JoinRoomConfirmViewModel.JoinRoomAction?> =
        combine(
            matrixClient.room.getById(roomId).mapNotNull { it?.membership },
            matrixClient.room.getState(roomId, HistoryVisibilityEventContent::class).mapNotNull {
                it?.content?.historyVisibility
            },
            matrixClient.room.getState(roomId, JoinRulesEventContent::class).mapNotNull {
                it?.content
            }
        ) { membership, historyVisibility, joinRuleContent ->
            return@combine when {
                joinRuleContent.joinRule == JoinRulesEventContent.JoinRule.Public -> JoinRoomConfirmViewModel.JoinRoomAction.Join

                joinRuleContent.joinRule.isKnock -> JoinRoomConfirmViewModel.JoinRoomAction.Knock

                //Only show restricted action when there are room join conditions
                joinRuleContent.joinRule == JoinRulesEventContent.JoinRule.Restricted -> {
                    val allowConditionsRooms =
                        joinRuleContent.allow?.filter { it.type == JoinRulesEventContent.AllowCondition.AllowConditionType.RoomMembership }
                            ?.map { it.roomId }?.toSet()
                    if (allowConditionsRooms?.isNotEmpty() ?: false) {
                        JoinRoomConfirmViewModel.JoinRoomAction.Restricted(allowConditionsRooms)
                    } else {
                        JoinRoomConfirmViewModel.JoinRoomAction.Impossible
                    }
                }

                joinRuleContent.joinRule == JoinRulesEventContent.JoinRule.Private -> JoinRoomConfirmViewModel.JoinRoomAction.Impossible

                else -> {
                    null
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

}
