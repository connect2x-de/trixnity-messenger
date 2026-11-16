package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallParticipant

@TrixnityMessengerCallApi
internal sealed interface CallState {
    data class Message(val text: String, val isError: Boolean = false) : CallState

    data class Active(
        val featured: CallParticipant?,
        val thumbnails: List<CallParticipant>,
        val microphoneEnabled: Boolean,
        val cameraEnabled: Boolean,
        val messages: List<Message>,
    ) : CallState
}
