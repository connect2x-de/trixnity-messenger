package de.connect2x.livekitnity.api.room.track

import de.connect2x.livekitnity.api.events.EventListenable
import de.connect2x.livekitnity.api.events.TrackPublicationEvent
import kotlinx.coroutines.flow.StateFlow

public interface TrackPublication {
  public val track: StateFlow<Track?>
  public val name: String
  public val sid: String
  public val kind: Track.Kind
  public val events: EventListenable<TrackPublicationEvent>
  public val muted: StateFlow<Boolean>
  public val subscribed: Boolean
  public val simulcasted: Boolean?
  public val dimensions: Track.Dimensions?
  public val source: Track.Source
  public val mimeType: String?
}
