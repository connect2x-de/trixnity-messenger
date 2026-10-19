package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.compose.state.rememberConnectionState
import de.connect2x.livekitnity.compose.state.rememberLocalMedia
import de.connect2x.trixnity.core.MSC4143
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.call.CallState
import de.connect2x.trixnity.messenger.viewmodel.room.call.RoomCallViewModel

@OptIn(MSC4143::class, TrixnityMessengerCallApi::class)
@Composable
internal fun rememberCallState(viewModel: RoomCallViewModel): CallState {
    val connection by viewModel.publishing.collectAsState()
    val receiving by viewModel.receiving.collectAsState()
    val members by viewModel.members.collectAsState()
    val error by viewModel.error.collectAsState()
    val i18n = DI.get<I18nView>()
    error?.let {
        return CallState.Message(it, isError = true)
    }
    val publishing = connection ?: return CallState.Message(i18n.callJoining())
    return key(publishing.url, publishing.token, publishing.userId, publishing.encryptionInfo != null) {
        val users =
            remember(members) {
                members.mapKeys { (member, _) -> matrixRtcParticipantIdentity(member.first, member.second) }
            }
        val localIdentity =
            remember(publishing.userId, publishing.memberId) {
                matrixRtcParticipantIdentity(publishing.userId, publishing.memberId)
            }
        var mediaError by remember { mutableStateOf<String?>(null) }
        val rooms = rememberCallRooms(publishing, receiving) { mediaError = i18n.callConnectionError() }
        val publishingState by rememberConnectionState(rooms.publishing)
        val receivingStates = rooms.receiving.map { room -> key(room) { rememberConnectionState(room).value } }
        val message =
            when {
                publishingState in listOf(Room.State.CONNECTING, Room.State.DISCONNECTED) ->
                    CallState.Message(i18n.callJoining())

                Room.State.DISCONNECTED in receivingStates ->
                    CallState.Message(i18n.callParticipantsUnavailable(), isError = true)

                publishingState == Room.State.RECONNECTING || Room.State.RECONNECTING in receivingStates ->
                    CallState.Message(i18n.callReconnecting())

                else -> null
            }
        val participants = rememberCallParticipants(rooms, users, localIdentity)
        val media = rememberLocalMedia(rooms.publishing)
        val connected = publishingState == Room.State.CONNECTED
        val microphone =
            rememberCallMediaControl(rooms.publishing, media, Track.Source.MICROPHONE, connected, i18n) {
                mediaError = it
            }
        val camera =
            rememberCallMediaControl(rooms.publishing, media, Track.Source.CAMERA, connected, i18n) { mediaError = it }
        CallState.Active(
            featured = participants.firstOrNull(),
            thumbnails = participants.drop(1),
            microphone = microphone,
            camera = camera,
            messages = listOfNotNull(message, mediaError?.let { CallState.Message(it, isError = true) }),
        )
    }
}
