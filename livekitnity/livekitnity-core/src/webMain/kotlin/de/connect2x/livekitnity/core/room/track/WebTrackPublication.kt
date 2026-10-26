package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.TrackPublicationEvent
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.api.room.track.WebTrackPublication
import de.connect2x.livekitnity.core.SimpleEventListenable
import de.connect2x.livekitnity.core.platform.Converter
import de.connect2x.livekitnity.core.platform.EventStateFlow
import de.connect2x.livekitnity.core.platform.StateDescriptors
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

internal fun WebTrackPublication(
    trackPublication: livekit.TrackPublication,
    converter: Converter,
): WebTrackPublication {
  return WebTrackPublicationImpl(
      trackPublication = trackPublication,
      converter = converter,
  )
}

private class WebTrackPublicationImpl(
    private val trackPublication: livekit.TrackPublication,
    private val converter: Converter,
) : WebTrackPublication {

  override val track: StateFlow<WebTrack?> =
      EventStateFlow(
          eventEmitter = trackPublication,
          converter = converter,
          descriptor =
              StateDescriptors.TrackPublication.track.map { it(trackPublication.trackName) },
      )

  override val name: String
    get() = trackPublication.trackName

  override val sid: String
    get() = trackPublication.trackSid

  override val kind: Track.Kind
    get() = Converter.toCommon(trackPublication.kind)

  override val events: EventListenable<TrackPublicationEvent> =
      SimpleEventListenable(MutableSharedFlow())

  override val muted: StateFlow<Boolean> =
      EventStateFlow(
          eventEmitter = trackPublication,
          converter = converter,
          descriptor = StateDescriptors.TrackPublication.muted,
      )

  override val subscribed: Boolean
    get() = trackPublication.isSubscribed

  override val simulcasted: Boolean?
    get() = trackPublication.simulcasted

  override val dimensions: Track.Dimensions?
    get() = Converter.toCommon(trackPublication.dimensions ?: return null)

  override val source: Track.Source
    get() = Converter.toCommon(trackPublication.source)

  override val mimeType: String?
    get() = trackPublication.mimeType
}
