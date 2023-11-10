package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.NameEventContent

private val log = KotlinLogging.logger { }

interface RoomSettingsNameViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>,
    ): RoomSettingsNameViewModel {
        return RoomSettingsNameViewModelImpl(viewModelContext, selectedRoomId, error)
    }

    companion object : RoomSettingsNameViewModelFactory
}

interface RoomSettingsNameViewModel {
    val roomNameLoading: StateFlow<Boolean>

    /** only use this value when [roomNameLoading] is `false` */
    val roomName: MutableStateFlow<String>
    val roomNameIsBeingEdited: StateFlow<Boolean>
    val canChangeRoomName: StateFlow<Boolean>

    fun changeRoomName()
    fun cancelRoomNameChange()
}

open class RoomSettingsNameViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val error: MutableStateFlow<String?>,
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsNameViewModel {
    protected val roomNameIsCurrentlyChanging = MutableStateFlow(false)
    protected val roomNameState: StateFlow<RoomNameState> = combine(
        matrixClient.room.getById(selectedRoomId),
        roomNameIsCurrentlyChanging,
    ) { room, roomNameIsCurrentlyChanging ->
        log.trace { "roomNameIsCurrentlyChanging: $roomNameIsCurrentlyChanging, room.name: ${room?.name}" }
        if (roomNameIsCurrentlyChanging) {
            RoomNameState.Undetermined
        } else {
            if (room != null) {
                RoomNameState.Determined(room.name?.explicitName) // only explicit name is relevant here
            } else {
                RoomNameState.Undetermined
            }
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, RoomNameState.Undetermined)

    override val roomNameLoading: StateFlow<Boolean> = roomNameState.map { it is RoomNameState.Undetermined }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), true)
    override val roomName: MutableStateFlow<String> = MutableStateFlow("")
    override val roomNameIsBeingEdited = combine(roomName, roomNameState) { name, origName ->
        name != when (origName) {
            is RoomNameState.Undetermined -> ""
            is RoomNameState.Determined -> origName.name ?: ""
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val canChangeRoomName: StateFlow<Boolean> =
        matrixClient.user.canSendEvent<NameEventContent>(selectedRoomId)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false) // is used for changeRoomName()

    init {
        coroutineScope.launch {
            roomNameState.collect {
                if (it is RoomNameState.Determined && roomNameIsBeingEdited.value.not()) {
                    roomName.value = it.name ?: ""
                }
            }
        }
    }

    override fun changeRoomName() {
        log.debug { "attempt to change the room's name (can change: ${canChangeRoomName.value}, name changed: ${roomNameIsBeingEdited.value})" }
        if (canChangeRoomName.value && roomNameIsBeingEdited.value) {
            roomNameIsCurrentlyChanging.value = true
            coroutineScope.launch {
                try {
                    matrixClient.api.rooms.sendStateEvent(
                        selectedRoomId,
                        NameEventContent(roomName.value),
                        stateKey = ""
                    )
                        .onFailure {
                            log.error(it) { "cannot change the room name to '${roomName.value}'" }
                            error.value = i18n.settingsRoomChangeNameError()
                        }
                        .onSuccess {
                            log.debug { "changed room name to '${roomName.value}'" }
                            error.value = null
                        }
                } finally {
                    roomNameIsCurrentlyChanging.value = false
                }
            }
        }
    }

    override fun cancelRoomNameChange() {
        val nameState = roomNameState.value
        roomName.value = when (nameState) {
            is RoomNameState.Undetermined -> ""
            is RoomNameState.Determined -> nameState.name ?: ""
        }
    }
}

class PreviewRoomSettingsNameViewModel : RoomSettingsNameViewModel {
    override val roomNameLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val roomName: MutableStateFlow<String> = MutableStateFlow("room name")
    override val roomNameIsBeingEdited: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canChangeRoomName: MutableStateFlow<Boolean> = MutableStateFlow(true)

    override fun changeRoomName() {
    }

    override fun cancelRoomNameChange() {
    }
}