package de.connect2x.livekitnity.api.room.track

import org.w3c.dom.HTMLVideoElement

public interface WebVideoTrack : VideoTrack, WebTrack {
  public fun attach(element: HTMLVideoElement)

  public fun detach(element: HTMLVideoElement)
}
