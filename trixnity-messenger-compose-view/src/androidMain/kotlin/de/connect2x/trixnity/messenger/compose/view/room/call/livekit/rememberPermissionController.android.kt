package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallPermissionKind

@TrixnityMessengerCallApi
@Composable
internal actual fun rememberPermissionController(onResult: (Boolean) -> Unit): PermissionController {
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = onResult)
    return remember(launcher) {
        PermissionController { kind ->
            launcher.launch(
                when (kind) {
                    CallPermissionKind.CAMERA -> Manifest.permission.CAMERA
                    CallPermissionKind.MICROPHONE -> Manifest.permission.RECORD_AUDIO
                }
            )
        }
    }
}
