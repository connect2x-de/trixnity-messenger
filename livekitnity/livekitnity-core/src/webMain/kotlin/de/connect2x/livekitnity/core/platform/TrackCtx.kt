package de.connect2x.livekitnity.core.platform

import de.connect2x.livekitnity.api.events.TrackEvent
import de.connect2x.livekitnity.api.room.track.Track
import livekit.TrackDimensions

internal class TrackCtx(
    private val track: Track,
    override val eventEmitter: livekit.Track,
    private val emit: (TrackEvent) -> Unit,
) : EventCtx<livekit.Track, TrackEvent> {

  override fun onEvent(event: TrackEvent): Unit {
    emit(event)
  }

  fun visibilityChanged(isVisible: JsBoolean, source: JsAny): TrackEvent {
    return TrackEvent.VisibilityChanged(
        track,
        isVisible.toBoolean(),
    )
  }

  fun videoDimensionsChanged(
      dimensions: TrackDimensions,
      source: JsAny,
  ): TrackEvent {
    return TrackEvent.VideoDimensionsChanged(
        track,
        Converter.toCommon(dimensions),
    )
  }
}
