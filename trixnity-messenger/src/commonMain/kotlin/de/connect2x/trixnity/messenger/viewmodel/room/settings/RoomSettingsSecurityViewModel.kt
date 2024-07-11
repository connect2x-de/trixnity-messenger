package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

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
                        error.value = i18n.roomEncryptionEnableError(it.message?: "")
                    }
            }
        } else {
            log.error { "Failed to enable room E2E encryption: encryption was already enabled" }
            error.value = i18n.roomEncryptionEnableError(i18n.roomEncryptionAlreadyEnabled())
        }
    }
}

class PreviewRoomSettingsSecurityViewModel : RoomSettingsSecurityViewModel {
    override val isEncrypted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val canEnableEncryption: StateFlow<Boolean> = MutableStateFlow(false)

    override fun enableEncryption() {
    }
}
