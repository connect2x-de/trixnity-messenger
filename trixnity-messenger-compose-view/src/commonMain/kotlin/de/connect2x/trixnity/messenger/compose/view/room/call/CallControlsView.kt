package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.room.call.CallControlsViewModel

@TrixnityMessengerCallApi
interface CallControlsView {
    @Composable fun create(viewModel: CallControlsViewModel)
}

@TrixnityMessengerCallApi
class CallControlsViewImpl : CallControlsView {
    @Composable
    override fun create(viewModel: CallControlsViewModel) {
        CompactCall(viewModel)
    }
}
