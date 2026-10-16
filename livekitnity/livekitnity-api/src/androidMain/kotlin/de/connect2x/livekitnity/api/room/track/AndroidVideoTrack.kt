package de.connect2x.livekitnity.api.room.track

import livekit.org.webrtc.VideoSink

public interface AndroidVideoTrack : VideoTrack, AndroidTrack {
  public fun addRenderer(renderer: VideoSink)

  public fun removeRenderer(renderer: VideoSink)
}
