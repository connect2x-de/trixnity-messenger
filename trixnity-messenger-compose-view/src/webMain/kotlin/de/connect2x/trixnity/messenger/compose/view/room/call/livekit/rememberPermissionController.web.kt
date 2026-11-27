package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi

@TrixnityMessengerCallApi
@Composable
internal actual fun rememberPermissionController(onResult: (Boolean) -> Unit): PermissionController {
    val result by rememberUpdatedState(onResult)
    // TODO: Request browser microphone and camera permissions and report the result.
    return remember { PermissionController { result(true) } }
}
