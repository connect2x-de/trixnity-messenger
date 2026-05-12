package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.lognity.api.logger.error
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.user
import de.connect2x.trixnity.client.user.canSendEvent
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.events.m.room.EncryptionEventContent

interface RoomSettingsSecurityViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        selectedRoomId: RoomId,
        error: MutableStateFlow<String?>
    ): RoomSettingsSecurityViewModel =
        RoomSettingsSecurityViewModelImpl(
            viewModelContext,
            selectedRoomId,
            error
        )

    companion object : RoomSettingsSecurityViewModelFactory
}

interface RoomSettingsSecurityViewModel {
    val isChat: StateFlow<Boolean>
    val isEncrypted: StateFlow<Boolean>
    val canEnableEncryption: StateFlow<Boolean>
    val isEncryptionWarningOpen: StateFlow<Boolean>

    fun openEnableEncryptionWarning()
    fun closeEnableEncryptionWarning()
    fun enableEncryption()
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomSettingsSecurityViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val selectedRoomId: RoomId,
    private val error: MutableStateFlow<String?>
) : MatrixClientViewModelContext by viewModelContext, RoomSettingsSecurityViewModel {
    override val isChat: StateFlow<Boolean> = matrixClient.room.getById(selectedRoomId)
        .mapLatest { it?.isDirect == true }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val isEncrypted: StateFlow<Boolean> = matrixClient.room.getById(selectedRoomId)
        .mapLatest { room -> room?.encrypted == true }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val canEnableEncryption: StateFlow<Boolean> =
        combine(
            matrixClient.user.canSendEvent<EncryptionEventContent>(selectedRoomId),
            isEncrypted
        ) { canEncrypt, isEncrypted ->
            return@combine canEncrypt && !isEncrypted
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isEncryptionWarningOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun openEnableEncryptionWarning() {
        this.isEncryptionWarningOpen.value = true
    }

    override fun closeEnableEncryptionWarning() {
        this.isEncryptionWarningOpen.value = false
    }

    override fun enableEncryption() {
        log.debug { "enableRoomEncryption for $selectedRoomId" }
        if (canEnableEncryption.value) {
            coroutineScope.launch {
                val roomApiClient = matrixClient.api.room
                roomApiClient.sendStateEvent(selectedRoomId, EncryptionEventContent())
                    .onSuccess {
                        error.value = null
                    }
                    .onFailure {
                        log.error(it) { "Failed to enable room E2E encryption: ${it.message}" }
                        error.value = i18n.roomEncryptionEnableError()
                    }
            }
        } else {
            log.error { "Failed to enable room E2E encryption: encryption was already enabled" }
            error.value = i18n.roomEncryptionAlreadyEnabledError()
        }
    }
}

class PreviewRoomSettingsSecurityViewModel : RoomSettingsSecurityViewModel {
    override val isChat: StateFlow<Boolean> = MutableStateFlow(false)
    override val isEncrypted: StateFlow<Boolean> = MutableStateFlow(false)
    override val canEnableEncryption: StateFlow<Boolean> = MutableStateFlow(false)
    override val isEncryptionWarningOpen: StateFlow<Boolean> = MutableStateFlow(false)

    override fun openEnableEncryptionWarning() {}
    override fun closeEnableEncryptionWarning() {}
    override fun enableEncryption() {}
}
