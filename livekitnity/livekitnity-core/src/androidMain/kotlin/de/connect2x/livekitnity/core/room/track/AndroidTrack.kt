package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.TrackEvent
import de.connect2x.livekitnity.api.room.track.AndroidTrack
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.mapSharedNotNull
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.util.flow
import kotlinx.coroutines.flow.StateFlow

internal fun AndroidTrack(
    delegate: io.livekit.android.room.track.Track,
    converter: Converter,
): AndroidTrack {
  return AndroidTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidTrackImpl(
    private val delegate: io.livekit.android.room.track.Track,
    private val converter: Converter,
) : AndroidTrack {

  override val events: EventListenable<TrackEvent> =
      SimpleEventListenable(
          events = delegate.events.events.mapSharedNotNull(converter::toCommon),
      )

  override val name: String
    get() = delegate.name

  override val kind: Track.Kind
    get() = Converter.toCommon(delegate.kind)

  override val sid: Track.Sid?
    get() = Track.Sid(delegate.sid ?: return null)

  override val streamState: StateFlow<Track.StreamState>
    get() = delegate::streamState.flow.mapState { Converter.toCommon(it) }
}
