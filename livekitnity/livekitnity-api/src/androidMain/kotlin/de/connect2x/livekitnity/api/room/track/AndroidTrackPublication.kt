package de.connect2x.livekitnity.api.room.track

import kotlinx.coroutines.flow.StateFlow

public interface AndroidTrackPublication : TrackPublication {
  override val track: StateFlow<AndroidTrack?>
}
