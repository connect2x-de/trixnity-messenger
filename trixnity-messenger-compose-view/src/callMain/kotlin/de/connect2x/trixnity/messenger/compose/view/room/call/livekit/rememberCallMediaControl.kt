package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.compose.types.LocalMedia
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.call.CallMediaControl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class PermissionKind {
    CAMERA,
    MICROPHONE,
}

internal interface PermissionController {
    fun requestPermission(permissionKind: PermissionKind)
}

@Composable
internal expect fun rememberPermissionController(onResult: (PermissionKind, Boolean) -> Unit): PermissionController

@Composable
internal fun rememberCallMediaControl(
    room: Room,
    media: LocalMedia,
    source: Track.Source,
    connected: Boolean,
    i18n: I18nView,
    onError: (String?) -> Unit,
): CallMediaControl {
    val camera = source == Track.Source.CAMERA
    val active = if (camera) media.isCameraEnabled else media.isMicrophoneEnabled
    val permission = if (camera) PermissionKind.CAMERA else PermissionKind.MICROPHONE
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    fun toggle() {
        if (!scope.isActive || room.state.value != Room.State.CONNECTED || busy) return
        busy = true
        onError(null)
        scope.launch {
            try {
                if (room.state.value != Room.State.CONNECTED) return@launch
                if (camera) media.setCameraEnabled(!media.isCameraEnabled)
                else media.setMicrophoneEnabled(!media.isMicrophoneEnabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onError(if (camera) i18n.callCameraError() else i18n.callMicrophoneError())
            } finally {
                busy = false
            }
        }
    }

    val permissionController = rememberPermissionController { _, granted ->
        busy = false
        if (scope.isActive && room.state.value == Room.State.CONNECTED) {
            if (granted) toggle()
            else onError(if (camera) i18n.callCameraPermissionDenied() else i18n.callMicrophonePermissionDenied())
        }
    }

    return CallMediaControl(
        active = active,
        enabled = connected && !busy,
        onToggle = {
            busy = true
            permissionController.requestPermission(permission)
        },
    )
}
