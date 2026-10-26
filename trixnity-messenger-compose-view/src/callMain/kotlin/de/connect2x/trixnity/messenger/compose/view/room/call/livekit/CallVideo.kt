package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.ui.VideoTrackView

internal data class CallVideo(val room: Room, val track: TrackReference)

@Composable
internal fun CallVideoView(video: CallVideo, modifier: Modifier) {
    key(video.room, video.track.participant) {
        VideoTrackView(
            video.track,
            modifier,
            room = video.room,
            mirror = video.track.participant === video.room.localParticipant,
        )
    }
}
