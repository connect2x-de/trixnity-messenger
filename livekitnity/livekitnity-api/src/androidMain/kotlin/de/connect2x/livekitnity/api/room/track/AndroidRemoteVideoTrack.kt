package de.connect2x.livekitnity.api.room.track

import livekit.org.webrtc.VideoSink

public interface AndroidRemoteVideoTrack : RemoteVideoTrack, AndroidVideoTrack {
  public fun addRenderer(videoSink: VideoSink, videoSinkVisibility: VideoSinkVisibility)
}
