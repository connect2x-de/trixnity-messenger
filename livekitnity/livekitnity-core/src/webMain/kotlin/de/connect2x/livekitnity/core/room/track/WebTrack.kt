package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.TrackEvent
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.platform.Converter
import de.connect2x.livekitnity.core.platform.EventDescriptors
import de.connect2x.livekitnity.core.platform.EventSharedFlow
import de.connect2x.livekitnity.core.platform.EventStateFlow
import de.connect2x.livekitnity.core.platform.StateDescriptors
import de.connect2x.livekitnity.core.platform.TrackCtx
import kotlinx.coroutines.flow.StateFlow

internal fun WebTrack(
    track: livekit.Track,
    converter: Converter,
    trackName: String,
): WebTrack {
  return WebTrackImpl(
      track = track,
      converter = converter,
      trackName = trackName,
  )
}

private class WebTrackImpl(
    private val track: livekit.Track,
    private val converter: Converter,
    private val trackName: String,
) : WebTrack {

  override val name: String
    get() = trackName

  override val events: EventListenable<TrackEvent> =
      SimpleEventListenable(
          EventSharedFlow(
              makeCtx = { onEvent ->
                TrackCtx(
                    track = this,
                    eventEmitter = track,
                    emit = onEvent,
                )
              },
              descriptors = EventDescriptors.Track.all,
          ),
      )

  override val kind: Track.Kind
    get() = Converter.toCommon(track.kind)

  override val sid: Track.Sid?
    get() = Track.Sid(track.sid ?: return null)

  override val streamState: StateFlow<Track.StreamState> =
      EventStateFlow(
          eventEmitter = track,
          converter = converter,
          descriptor = StateDescriptors.Track.streamState,
      )
}
