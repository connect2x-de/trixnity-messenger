package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent

interface RoomSettingsHistoryVisibilityViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        historyVisibility: HistoryVisibilityEventContent.HistoryVisibility,
        error: MutableStateFlow<String?>,
    ): RoomSettingsHistoryVisibilityViewModel {
        return RoomSettingsHistoryVisibilityViewModelImpl(viewModelContext, selectedRoomId, historyVisibility, error)
    }
    companion object : RoomSettingsHistoryVisibilityViewModelFactory
}

interface RoomSettingsHistoryVisibilityViewModel {
    val roomHistoryVisibility: StateFlow<HistoryVisibilityEventContent.HistoryVisibility>
    val canChangeRoomHistoryVisibility: StateFlow<Boolean>

    fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility)
}

open class RoomSettingsHistoryVisibilityViewModelImpl(
    private val viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    historyVisibility: HistoryVisibilityEventContent.HistoryVisibility,
    private val error: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsHistoryVisibilityViewModel {

    private val _roomHistoryVisibilityState = MutableStateFlow(historyVisibility)
    override val roomHistoryVisibility: StateFlow<HistoryVisibilityEventContent.HistoryVisibility> = _roomHistoryVisibilityState
    override val canChangeRoomHistoryVisibility = MutableStateFlow(false)

    override fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        viewModelContext.coroutineScope.launch {
            try {
                viewModelContext.matrixClient.api.room.sendStateEvent(
                    selectedRoomId,
                    HistoryVisibilityEventContent(newVisibility),
                    stateKey = ""
                )
                    .onFailure {
                        error.value = "Failed to change room history visibility: ${it.message}"
                    }
                    .onSuccess {
                        _roomHistoryVisibilityState.value = newVisibility
                        error.value = null
                    }
            } catch (e: Exception) {
                error.value = "Exception occurred while changing room history visibility: ${e.message}"
            }
        }
    }
}

class PreviewRoomSettingsHistoryVisibilityViewModel : RoomSettingsHistoryVisibilityViewModel {
    override val roomHistoryVisibility = MutableStateFlow(HistoryVisibilityEventContent.HistoryVisibility.SHARED)
    override val canChangeRoomHistoryVisibility = MutableStateFlow(true)

    override fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        roomHistoryVisibility.value = newVisibility
    }
}