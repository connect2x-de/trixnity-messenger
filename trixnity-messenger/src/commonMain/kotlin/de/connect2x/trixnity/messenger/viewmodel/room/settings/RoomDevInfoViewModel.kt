package de.connect2x.trixnity.messenger.viewmodel.room.settings

import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext

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

    fun back()
}

class RoomDevInfoViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    override val roomId: RoomId,
    private val onBack: () -> Unit,
) : RoomDevInfoViewModel, MatrixClientViewModelContext by viewModelContext {

    private val backCallback = BackCallback { onBack() }

    init {
        registerBackCallback(backCallback)
    }

    override fun back() {
        onBack()
    }
}
