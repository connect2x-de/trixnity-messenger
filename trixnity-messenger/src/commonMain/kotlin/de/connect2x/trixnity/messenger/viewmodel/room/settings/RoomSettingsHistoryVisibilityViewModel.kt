package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.getState
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.HistoryVisibilityEventContent
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger { }

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
    val isHistoryVisibilityChanging: StateFlow<Boolean>
    val availableRoomHistoryVisibilityLevels: StateFlow<List<HistoryVisibilityEventContent.HistoryVisibility>>
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
    override val availableRoomHistoryVisibilityLevels = MutableStateFlow(
        listOf(
            HistoryVisibilityEventContent.HistoryVisibility.SHARED,
            HistoryVisibilityEventContent.HistoryVisibility.JOINED,
            HistoryVisibilityEventContent.HistoryVisibility.INVITED,
            HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
        )
    )

    override fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        log.debug { "changeRoomHistoryVisibility for $selectedRoomId to $newVisibility" }
        if (canChangeRoomHistoryVisibility.value) {
            if (availableRoomHistoryVisibilityLevels.value.any { it == newVisibility }) {
                coroutineScope.launch {
                    isHistoryVisibilityChanging.value = true
                    matrixClient.api.room.sendStateEvent(
                        selectedRoomId,
                        HistoryVisibilityEventContent(newVisibility),
                        stateKey = ""

                    )
                        .onFailure {
                            log.error(it) { "Failed to change room history visibility: ${it.message}" }
                            error.value = i18n.settingsRoomHistoryVisibilityChangeError()
                            isHistoryVisibilityChanging.value = false
                        }
                        .onSuccess {
                            error.value = null
                            withTimeoutOrNull(5.seconds) {
                                matrixClient.room.getState<HistoryVisibilityEventContent>(selectedRoomId)
                                    .first { it?.content?.historyVisibility == newVisibility }
                            }
                            isHistoryVisibilityChanging.value = false
                        }
                }
            } else {
                log.error { "new visibility $newVisibility could not be set, since the available visibility levels are ${availableRoomHistoryVisibilityLevels.value}" }
                error.value = i18n.settingsRoomHistoryVisibilityChangeError()
                isHistoryVisibilityChanging.value = false
            }
        } else {
            log.error { "Insufficient power level to change room history visibility" }
            error.value = i18n.settingsRoomHistoryVisibilityInsufficientPowerLevel()
            isHistoryVisibilityChanging.value = false
        }
    }
}

class PreviewRoomSettingsHistoryVisibilityViewModel : RoomSettingsHistoryVisibilityViewModel {
    override val roomHistoryVisibility: MutableStateFlow<HistoryVisibilityEventContent.HistoryVisibility> =
        MutableStateFlow(HistoryVisibilityEventContent.HistoryVisibility.SHARED)
    override val canChangeRoomHistoryVisibility: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isHistoryVisibilityChanging: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val availableRoomHistoryVisibilityLevels: MutableStateFlow<List<HistoryVisibilityEventContent.HistoryVisibility>> =
        MutableStateFlow(
            listOf(
                HistoryVisibilityEventContent.HistoryVisibility.SHARED,
                HistoryVisibilityEventContent.HistoryVisibility.JOINED,
                HistoryVisibilityEventContent.HistoryVisibility.INVITED,
                HistoryVisibilityEventContent.HistoryVisibility.WORLD_READABLE
            )
        )

    override fun changeRoomHistoryVisibility(newVisibility: HistoryVisibilityEventContent.HistoryVisibility) {
        roomHistoryVisibility.value = newVisibility
    }
}

