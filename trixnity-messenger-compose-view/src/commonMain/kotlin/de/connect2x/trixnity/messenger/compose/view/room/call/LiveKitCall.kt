package de.connect2x.trixnity.messenger.compose.view.room.call

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallSessionState

@OptIn(TrixnityMessengerCallApi::class)
@Composable
internal expect fun LiveKitCall(
    state: CallSessionState,
    onLeave: () -> Unit,
    onMicrophone: () -> Unit,
    onCamera: () -> Unit,
    onBackground: () -> Unit,
)
