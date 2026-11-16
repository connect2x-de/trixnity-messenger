package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi

@TrixnityMessengerCallApi
@Composable
internal actual fun rememberPermissionController(onResult: (Boolean) -> Unit): PermissionController {
    throw IllegalStateException("Call permission requests are unsupported on this platform")
}
