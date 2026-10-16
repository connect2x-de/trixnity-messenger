package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel

@OptIn(TrixnityMessengerCallApi::class)
@Composable
internal actual fun LiveKitCall(viewModel: RoomCallViewModel) {
    // LiveKit is not currently supported on this platform
}
