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
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.lognity.api.logger.warn
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.call.CallVideo

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.room.call.livekit.CallVideo")

@TrixnityMessengerCallApi internal data class LiveKitCallVideo(val room: Room, val track: TrackReference) : CallVideo

@TrixnityMessengerCallApi
@Composable
internal fun CallVideoView(handle: CallVideo, modifier: Modifier, backgroundColor: Color) {
    val video = handle as? LiveKitCallVideo
    if (video == null) {
        log.warn { "Unsupported call video type: ${handle::class}" }
        return
    }
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
