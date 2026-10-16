package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidRemoteVideoTrack
import de.connect2x.livekitnity.api.room.track.AndroidVideoTrack
import de.connect2x.livekitnity.api.room.track.VideoSinkVisibility
import de.connect2x.livekitnity.core.platform.Converter
import livekit.org.webrtc.VideoSink

internal fun AndroidRemoteVideoTrack(
    delegate: io.livekit.android.room.track.RemoteVideoTrack,
    converter: Converter,
): AndroidRemoteVideoTrack {
  return AndroidRemoteVideoTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidRemoteVideoTrackImpl(
    private val delegate: io.livekit.android.room.track.RemoteVideoTrack,
    converter: Converter,
) :
    AndroidRemoteVideoTrack,
    AndroidVideoTrack by AndroidVideoTrack(
        delegate = delegate,
        converter = converter,
    ) {
  override fun addRenderer(videoSink: VideoSink, videoSinkVisibility: VideoSinkVisibility) {
    return delegate.addRenderer(videoSink, Converter.fromCommon(videoSinkVisibility))
  }
}
