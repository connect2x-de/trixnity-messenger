package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.compose.state.rememberTrackMuted
import de.connect2x.livekitnity.compose.state.rememberTracks
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.trixnity.messenger.compose.view.room.call.CallParticipant
import de.connect2x.trixnity.messenger.viewmodel.UserInfoElement

private data class LiveKitParticipant(
    val id: String,
    val room: Room,
    val camera: TrackReference,
    val hasPublication: Boolean,
)

@Composable
internal fun rememberCallParticipants(
    rooms: CallRooms,
    members: Map<String, UserInfoElement>,
    localIdentity: String,
): List<CallParticipant> {
    val allRooms = listOf(rooms.publishing) + rooms.receiving
    val participants =
        allRooms
            .flatMap { room ->
                key(room) {
                    val tracks by
                        rememberTracks(
                            sources = Track.Source.entries,
                            usePlaceholders = setOf(Track.Source.CAMERA),
                            passedRoom = room,
                            onlySubscribed = false,
                        )
                    val publishers = tracks.filterNot { it.isPlaceholder() }.map { it.participant }.toSet()
                    tracks
                        .filter { it.source == Track.Source.CAMERA }
                        .filter { it.participant !== room.localParticipant || room === rooms.publishing }
                        .distinctBy { it.participant }
                        .mapNotNull { camera ->
                            key(camera.participant) {
                                val participant = camera.participant
                                val identity by participant.identity.collectAsState()
                                val id = if (participant === room.localParticipant) localIdentity else identity?.value
                                id?.let {
                                    LiveKitParticipant(
                                        id = it,
                                        room = room,
                                        camera = camera,
                                        hasPublication = participant in publishers,
                                    )
                                }
                            }
                        }
                }
            }
            .sortedByDescending { it.hasPublication }
            .distinctBy { it.id }
            .associateBy { it.id }

    return members
        .map { (id, user) ->
            val item = participants[id]
            val participant = item?.camera?.participant
            key(id, item?.room, participant) {
                val microphoneEnabled = participant?.isMicrophoneEnabled?.collectAsState()?.value ?: false
                val cameraMuted = item?.let { rememberTrackMuted(it.camera).value } ?: true
                val lastSpokeAt = participant?.lastSpokeAt?.collectAsState()?.value ?: 0L
                CallParticipant(
                    id = id,
                    user = user,
                    isLocal = id == localIdentity,
                    microphoneEnabled = microphoneEnabled,
                    video = if (item != null && !cameraMuted) CallVideo(item.room, item.camera) else null,
                ) to lastSpokeAt
            }
        }
        .sortedWith(compareBy<Pair<CallParticipant, Long>> { it.first.isLocal }.thenByDescending { it.second })
        .map { it.first }
}
