package de.connect2x.livekitnity.api.room.track

import kotlinx.coroutines.flow.StateFlow

public interface VideoSinkVisibility {

  public val visibilityState: StateFlow<State>

  public data class State(
      val isVisible: Boolean,
      val size: Track.Dimensions,
  )
}
