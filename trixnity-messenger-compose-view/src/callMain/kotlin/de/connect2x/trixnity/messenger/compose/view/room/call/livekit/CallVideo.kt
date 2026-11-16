package de.connect2x.trixnity.messenger.compose.view.room.call.livekit

import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.compose.types.ScaleType
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.ui.VideoTrackView

internal data class CallVideo(val room: Room, val track: TrackReference)

@Composable
internal fun CallVideoView(video: CallVideo, modifier: Modifier, backgroundColor: Color) {
    key(video.room, video.track.participant) {
        VideoTrackView(
            video.track,
            modifier,
            room = video.room,
            scaleType = ScaleType.FitInside,
            backgroundColor = backgroundColor,
            shape = CardDefaults.shape,
            mirror = video.track.participant === video.room.localParticipant,
        )
    }
}
