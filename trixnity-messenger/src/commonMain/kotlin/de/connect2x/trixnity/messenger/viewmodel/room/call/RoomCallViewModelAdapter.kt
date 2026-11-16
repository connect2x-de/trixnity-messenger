package de.connect2x.trixnity.messenger.viewmodel.room.call

import com.arkivanov.essenty.lifecycle.doOnDestroy
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallSignalling
import de.connect2x.trixnity.messenger.util.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext

@TrixnityMessengerCallApi
interface RoomCallViewModel : CallSignalling {
    fun back()
}

@TrixnityMessengerCallApi
internal class RoomCallViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val onBack: () -> Unit,
    private val signalling: CallSignalling,
) : RoomCallViewModel, CallSignalling by signalling {
    init {
        viewModelContext.registerBackCallback(BackCallback(onBack = ::back))
        viewModelContext.lifecycle.doOnDestroy { close() }
    }

    override fun back() {
        close()
        onBack()
    }
}
