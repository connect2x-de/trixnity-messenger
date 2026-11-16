package de.connect2x.trixnity.messenger.compose.view.room.call

import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallConnectionState
import de.connect2x.trixnity.messenger.call.CallMediaError
import de.connect2x.trixnity.messenger.call.CallSessionState
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView

@TrixnityMessengerCallApi
internal fun CallSessionState.toCallState(i18n: I18nView): CallState {
    val status =
        when (media.connection) {
            CallConnectionState.CONNECTING -> CallState.Message(i18n.callJoining())
            CallConnectionState.RECONNECTING -> CallState.Message(i18n.callReconnecting())
            else -> null
        }
    val error =
        when (media.error) {
            CallMediaError.CONNECTION -> i18n.callConnectionError()
            CallMediaError.CAMERA -> i18n.callCameraError()
            CallMediaError.MICROPHONE -> i18n.callMicrophoneError()
            CallMediaError.CAMERA_PERMISSION -> i18n.callCameraPermissionDenied()
            CallMediaError.MICROPHONE_PERMISSION -> i18n.callMicrophonePermissionDenied()
            null -> null
        }
    return CallState.Active(
        media.participants.firstOrNull(),
        media.participants.drop(1),
        CallMediaControl(media.microphoneEnabled, true),
        CallMediaControl(media.cameraEnabled, true),
        listOfNotNull(
            this.error?.let { CallState.Message(it, true) },
            status,
            error?.let { CallState.Message(it, true) },
            if (media.participantsUnavailable) CallState.Message(i18n.callParticipantsUnavailable(), true) else null,
        ),
    )
}
