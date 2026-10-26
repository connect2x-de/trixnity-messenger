package de.connect2x.livekitnity.api.events

import de.connect2x.livekitnity.api.room.track.Track

public sealed class TrackEvent(public val track: Track) : Event() {

  public class VisibilityChanged(track: Track, public val isVisible: Boolean) : TrackEvent(track)

  public class VideoDimensionsChanged(track: Track, public val newDimensions: Track.Dimensions) :
      TrackEvent(track)
}
