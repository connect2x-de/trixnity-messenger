package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent

interface RoomSettingsHistoryVisibilityViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>,
    ): RoomSettingsHistoryVisibilityViewModel {
        return RoomSettingsHistoryVisibilityViewModelImpl(viewModelContext, selectedRoomId, error)
    }
    companion object : RoomSettingsHistoryVisibilityViewModelFactory
}

interface RoomSettingsHistoryVisibilityViewModel {
    val roomHistoryVisibility: StateFlow<HistoryVisibilityEventContent.HistoryVisibility>
    val canChangeRoomHistoryVisibility: StateFlow<Boolean>
    val isHistoryVisibilityChanging : StateFlow<Boolean>
    val roomVisibilityLevels: List<HistoryVisibilityEventContent.HistoryVisibility>
    fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility)
}

class RoomSettingsHistoryVisibilityViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val error: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsHistoryVisibilityViewModel {
    override val roomHistoryVisibility = matrixClient.room.getState<HistoryVisibilityEventContent>(selectedRoomId)
        .map { it?.content?.historyVisibility ?: HistoryVisibilityEventContent.HistoryVisibility.SHARED }
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(),
            HistoryVisibilityEventContent.HistoryVisibility.SHARED
        )
    override val canChangeRoomHistoryVisibility =
        matrixClient.user.canSendEvent<HistoryVisibilityEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val isHistoryVisibilityChanging = MutableStateFlow(false)
    override val roomVisibilityLevels = listOf(
        HistoryVisibilityEventContent.HistoryVisibility.SHARED,
        HistoryVisibilityEventContent.HistoryVisibility.JOINED,
        HistoryVisibilityEventContent.HistoryVisibility.INVITED,
        HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
    )

    override fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        if (canChangeRoomHistoryVisibility.value) {
            viewModelContext.coroutineScope.launch {
                try {
                    isHistoryVisibilityChanging.value = true
                    viewModelContext.matrixClient.api.room.sendStateEvent(
                        selectedRoomId,
                        HistoryVisibilityEventContent(newVisibility),
                        stateKey = ""

                    )
                        .onFailure {
                            error.value = "Failed to change room history visibility: ${it.message}"
                            isHistoryVisibilityChanging.value = false
                        }
                        .onSuccess {
                            error.value = null
                            isHistoryVisibilityChanging.value = false
                        }
                } catch (e: Exception) {
                    error.value = "Exception occurred while changing room history visibility: ${e.message}"
                }
            }
        }
        else {
            error.value = "Insufficient power level to change room history visibility"
            isHistoryVisibilityChanging.value = false
        }
    }
}

class PreviewRoomSettingsHistoryVisibilityViewModel : RoomSettingsHistoryVisibilityViewModel {
    override val roomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility> = MutableStateFlow(HistoryVisibilityEventContent.HistoryVisibility.SHARED)
    override val canChangeRoomHistoryVisibility: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isHistoryVisibilityChanging: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val roomVisibilityLevels: List<HistoryVisibilityEventContent.HistoryVisibility> = listOf(
        HistoryVisibilityEventContent.HistoryVisibility.SHARED,
        HistoryVisibilityEventContent.HistoryVisibility.JOINED,
        HistoryVisibilityEventContent.HistoryVisibility.INVITED,
        HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
    )

    override fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        roomHistoryVisibility.value = newVisibility
    }
}

