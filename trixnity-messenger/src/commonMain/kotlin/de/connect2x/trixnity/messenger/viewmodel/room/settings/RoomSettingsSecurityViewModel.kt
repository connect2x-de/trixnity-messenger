package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.canSendEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.EncryptionEventContent

private val log = KotlinLogging.logger { }

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
    val isEncrypted: StateFlow<Boolean>
    val canEnableEncryption: StateFlow<Boolean>
    val isEncryptionWarningOpen: StateFlow<Boolean>
    val encryptionWarningTitle: StateFlow<String>
    val encryptionWarningMessage: StateFlow<String>

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
    override val isEncrypted: StateFlow<Boolean> = matrixClient.room.getById(selectedRoomId)
        .mapLatest { room -> requireNotNull(room).encrypted }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)
    override val canEnableEncryption: StateFlow<Boolean> =
        combine(
            matrixClient.user.canSendEvent<EncryptionEventContent>(selectedRoomId),
            isEncrypted
        ) { canEncrypt, isEncrypted ->
            return@combine canEncrypt && !isEncrypted
        }.stateIn(coroutineScope, SharingStarted.Eagerly, false)
    override val isEncryptionWarningOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val encryptionWarningTitle: MutableStateFlow<String> = MutableStateFlow("")
    override val encryptionWarningMessage: MutableStateFlow<String> = MutableStateFlow("")

    init {
        coroutineScope.launch {
            matrixClient.room.getById(selectedRoomId).filterNotNull().collect { room ->
                if (room.isDirect) {
                    encryptionWarningTitle.value = i18n.roomSettingsEnableEncryptionWarningTitleChat()
                    encryptionWarningMessage.value = i18n.roomSettingsEnableEncryptionWarningMessageChat()
                } else {
                    encryptionWarningTitle.value = i18n.roomSettingsEnableEncryptionWarningTitleGroup()
                    encryptionWarningMessage.value = i18n.roomSettingsEnableEncryptionWarningMessageGroup()
                }
            }
        }
    }

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
    override val isEncrypted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canEnableEncryption: StateFlow<Boolean> = MutableStateFlow(false)
    override val isEncryptionWarningOpen: StateFlow<Boolean> = MutableStateFlow(false)
    override val encryptionWarningTitle: MutableStateFlow<String> = MutableStateFlow("")
    override val encryptionWarningMessage: MutableStateFlow<String> = MutableStateFlow("")

    override fun openEnableEncryptionWarning() {}
    override fun closeEnableEncryptionWarning() {}
    override fun enableEncryption() {}
}
