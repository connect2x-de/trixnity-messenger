package de.connect2x.livekitnity.compose.types

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.livekitnity.api.room.Room

public interface VideoTrackRenderer {
  @Composable
  public fun Content(
      trackReference: TrackReference?,
      modifier: Modifier = Modifier,
      room: Room? = null,
      mirror: Boolean = false,
      scaleType: ScaleType = ScaleType.Fill,
  )
}
