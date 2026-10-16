package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.trixnity.messenger.compose.view.room.call.livekit.CallVideo
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

internal sealed interface CallState {
    data class Message(val text: String, val isError: Boolean = false) : CallState

    data class Active(
        val featured: CallParticipant?,
        val thumbnails: List<CallParticipant>,
        val microphone: CallMediaControl,
        val camera: CallMediaControl,
        val messages: List<Message>,
    ) : CallState
}

internal data class CallParticipant(
    val id: String,
    val user: UserInfoElement,
    val isLocal: Boolean,
    val microphoneEnabled: Boolean,
    val video: CallVideo?,
)

internal data class CallMediaControl(val active: Boolean, val enabled: Boolean, val onToggle: () -> Unit)
