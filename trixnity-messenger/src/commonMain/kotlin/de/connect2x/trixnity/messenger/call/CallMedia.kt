package de.connect2x.trixnity.messenger.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

@TrixnityMessengerCallApi
enum class CallPermissionKind {
    CAMERA,
    MICROPHONE,
}

@TrixnityMessengerCallApi
enum class CallConnectionState {
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED,
}

@TrixnityMessengerCallApi
enum class CallMediaError {
    CONNECTION,
    CAMERA,
    MICROPHONE,
    CAMERA_PERMISSION,
    MICROPHONE_PERMISSION,
}

/** Opaque media handle. Only the platform renderer interprets its implementation. */
@TrixnityMessengerCallApi interface CallVideo

@TrixnityMessengerCallApi
data class CallParticipant(
    val id: String,
    val user: UserInfoElement,
    val isByMe: Boolean,
    val microphoneEnabled: Boolean,
    val isSpeaking: Boolean,
    val video: CallVideo?,
)

@TrixnityMessengerCallApi
data class CallMediaState(
    val connection: CallConnectionState = CallConnectionState.CONNECTING,
    val participants: List<CallParticipant> = emptyList(),
    val microphoneEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val changingMedia: Boolean = false,
    val error: CallMediaError? = null,
    val participantsUnavailable: Boolean = false,
)

@TrixnityMessengerCallApi
interface CallMedia {
    val state: StateFlow<CallMediaState>

    fun onPermissionRequestResult(kind: CallPermissionKind, granted: Boolean)

    fun toggleMicrophone()

    fun toggleCamera()
}

@TrixnityMessengerCallApi
fun interface CallMediaFactory {
    fun create(scope: CoroutineScope, rtc: CallSignalling, requestPermission: (CallPermissionKind) -> Unit): CallMedia
}
