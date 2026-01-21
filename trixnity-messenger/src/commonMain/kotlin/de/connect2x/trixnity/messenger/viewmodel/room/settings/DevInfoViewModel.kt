package de.connect2x.trixnity.messenger.viewmodel.room.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import net.folivo.trixnity.core.model.RoomId

interface DevInfoViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        roomId: RoomId,
        onBack: () -> Unit,
    ): DevInfoViewModel {
        return DevInfoViewModelImpl(
            viewModelContext,
            roomId,
            onBack,
        )
    }

    companion object : DevInfoViewModelFactory
}

interface DevInfoViewModel {
    val roomId: RoomId;
    fun back()
}

class DevInfoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val roomId: RoomId,
    private val onBack: () -> Unit,
) : DevInfoViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback {
        onBack()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun back() {
        onBack()
    }
}
