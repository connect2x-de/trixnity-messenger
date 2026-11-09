package de.connect2x.livekitnity.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.livekitnity.api.room.Room
import de.connect2x.livekitnity.compose.local.VideoTrackRendererLocal
import de.connect2x.livekitnity.compose.types.ScaleType
import de.connect2x.livekitnity.compose.types.TrackReference
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer

@Composable
public fun VideoTrackView(
    trackReference: TrackReference?,
    modifier: Modifier = Modifier,
    room: Room? = null,
    mirror: Boolean = false,
    scaleType: ScaleType = ScaleType.Fill,
    videoTrackRenderer: VideoTrackRenderer = VideoTrackRendererLocal.current,
) {
  videoTrackRenderer.Content(
      trackReference = trackReference,
      modifier = modifier,
      room = room,
      mirror = mirror,
      scaleType = scaleType,
  )
}
