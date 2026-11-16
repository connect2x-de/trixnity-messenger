package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallMediaError
import de.connect2x.trixnity.messenger.call.CallPermissionKind
import kotlinx.coroutines.flow.MutableStateFlow

@TrixnityMessengerCallApi
internal class CallMediaControls(private val requestPermission: (CallPermissionKind) -> Unit) {
    val microphoneEnabled = MutableStateFlow(false)
    val cameraEnabled = MutableStateFlow(false)
    val microphonePermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted = MutableStateFlow(false)
    val error = MutableStateFlow<CallMediaError?>(null)
    private var pendingPermission: CallPermissionKind? = null

    fun toggle(kind: CallPermissionKind) {
        val enabled = enabled(kind)
        enabled.value = !enabled.value
        error.value = null
        requestNextPermission()
    }

    fun onPermissionRequestResult(kind: CallPermissionKind, granted: Boolean) {
        if (pendingPermission != kind) return
        pendingPermission = null
        permissionGranted(kind).value = granted
        if (!granted && enabled(kind).value) {
            enabled(kind).value = false
            error.value =
                if (kind == CallPermissionKind.CAMERA) CallMediaError.CAMERA_PERMISSION
                else CallMediaError.MICROPHONE_PERMISSION
        }
        requestNextPermission()
    }

    fun onMediaChangeFailure(kind: CallPermissionKind, requested: Boolean, actual: Boolean) {
        // Preserve a newer choice made while LiveKit was applying the previous one.
        enabled(kind).compareAndSet(requested, actual)
        error.value = if (kind == CallPermissionKind.CAMERA) CallMediaError.CAMERA else CallMediaError.MICROPHONE
    }

    private fun enabled(kind: CallPermissionKind) =
        if (kind == CallPermissionKind.CAMERA) cameraEnabled else microphoneEnabled

    private fun permissionGranted(kind: CallPermissionKind) =
        if (kind == CallPermissionKind.CAMERA) cameraPermissionGranted else microphonePermissionGranted

    private fun requestNextPermission() {
        if (pendingPermission != null) return
        val kind =
            when {
                microphoneEnabled.value && !microphonePermissionGranted.value -> CallPermissionKind.MICROPHONE
                cameraEnabled.value && !cameraPermissionGranted.value -> CallPermissionKind.CAMERA
                else -> return
            }
        pendingPermission = kind
        requestPermission(kind)
    }
}
