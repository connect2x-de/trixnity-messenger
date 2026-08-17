package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.client.room
import de.connect2x.trixnity.client.store.OlmCryptoStore
import de.connect2x.trixnity.client.store.StoreTransactionManager
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface RoomDevInfoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onBack: () -> Unit,
    ): RoomDevInfoViewModel {
        return RoomDevInfoViewModelImpl(viewModelContext, roomId, onBack)
    }

    companion object : RoomDevInfoViewModelFactory
}

interface RoomDevInfoViewModel {
    val roomId: RoomId
    val showResetEncryptionButton: StateFlow<Boolean>

    fun resetEncryption()

    fun back()
}

class RoomDevInfoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val roomId: RoomId,
    private val onBack: () -> Unit,
) : RoomDevInfoViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback { onBack() }

    private val storeTransactionManager: StoreTransactionManager = matrixClient.di.get()
    private val olmCryptoStore: OlmCryptoStore = matrixClient.di.get()

    override val showResetEncryptionButton =
        matrixClient.room
            .getById(roomId)
            .map { it?.encrypted == true }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    init {
        registerBackCallback(backCallback)
    }

    override fun resetEncryption() {
        coroutineScope.launch {
            if (showResetEncryptionButton.value) {
                storeTransactionManager.writeTransaction { olmCryptoStore.updateOutboundMegolmSession(roomId) { null } }
            }
        }
    }

    override fun back() {
        onBack()
    }
}
