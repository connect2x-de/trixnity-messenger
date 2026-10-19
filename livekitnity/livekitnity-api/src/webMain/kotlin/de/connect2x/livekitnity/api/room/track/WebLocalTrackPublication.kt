package de.connect2x.livekitnity.api.room.track

import kotlinx.coroutines.flow.StateFlow

public interface WebLocalTrackPublication : LocalTrackPublication, WebTrackPublication {
  override val track: StateFlow<WebTrack?>
}
