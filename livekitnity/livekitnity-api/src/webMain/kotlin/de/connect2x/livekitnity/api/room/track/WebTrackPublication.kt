package de.connect2x.livekitnity.api.room.track

import kotlinx.coroutines.flow.StateFlow

public interface WebTrackPublication : TrackPublication {
  override val track: StateFlow<WebTrack?>
}
