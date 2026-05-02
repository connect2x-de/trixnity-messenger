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

    companion object JoinRoomViewModelFactory
}

interface JoinRoomConfirmViewModel {
    val actionNecessary: StateFlow<JoinRoomAction?>

    enum class JoinRoomAction {
        JOIN, KNOCK, IMPOSSIBLE
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
                it?.content?.joinRule
            }
        ) { membership, historyVisibility, joinRule ->
            return@combine when {
                joinRule.isKnock -> JoinRoomConfirmViewModel.JoinRoomAction.KNOCK
                else -> {
                    null
                }
            }
        }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

}
