package de.connect2x.livekitnity.api.room.participant

import de.connect2x.livekitnity.api.room.track.Track

public interface TrackPublishOptions {
  public val name: String?
  public val source: Track.Source?
  public val stream: String?
}
