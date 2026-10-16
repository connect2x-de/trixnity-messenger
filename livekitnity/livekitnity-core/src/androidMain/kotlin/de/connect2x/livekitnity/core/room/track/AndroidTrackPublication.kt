package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.TrackPublicationEvent
import de.connect2x.livekitnity.api.room.track.AndroidTrack
import de.connect2x.livekitnity.api.room.track.AndroidTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.mapSharedNotNull
import de.connect2x.livekitnity.core.mapState
import de.connect2x.livekitnity.core.platform.Converter
import io.livekit.android.util.flow
import kotlinx.coroutines.flow.StateFlow

internal fun AndroidTrackPublication(
    delegate: io.livekit.android.room.track.TrackPublication,
    converter: Converter,
): AndroidTrackPublication {
  return AndroidTrackPublicationImpl(delegate, converter)
}

private class AndroidTrackPublicationImpl(
    private val delegate: io.livekit.android.room.track.TrackPublication,
    private val converter: Converter,
) : AndroidTrackPublication {
  override val track: StateFlow<AndroidTrack?>
    get() = delegate::track.flow.mapState { it?.let { converter.toCommon(it) } }

  override val name: String
    get() = delegate.name

  override val sid: String
    get() = delegate.sid

  override val kind: Track.Kind
    get() = Converter.toCommon(delegate.kind)

  override val events: EventListenable<TrackPublicationEvent> =
      SimpleEventListenable(
          events = delegate.events.events.mapSharedNotNull(converter::toCommon),
      )

  override val muted: StateFlow<Boolean>
    get() = delegate::muted.flow

  override val subscribed: Boolean
    get() = delegate.subscribed

  override val simulcasted: Boolean?
    get() = delegate.simulcasted

  override val dimensions: Track.Dimensions?
    get() = delegate.dimensions?.let { Converter.toCommon(it) }

  override val source: Track.Source
    get() = Converter.toCommon(delegate.source)

  override val mimeType: String?
    get() = delegate.mimeType
}
