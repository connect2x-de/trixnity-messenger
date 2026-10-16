package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidTrack
import de.connect2x.livekitnity.api.room.track.AndroidVideoTrack
import de.connect2x.livekitnity.core.platform.Converter
import livekit.org.webrtc.VideoSink

internal fun AndroidVideoTrack(
    delegate: io.livekit.android.room.track.VideoTrack,
    converter: Converter,
): AndroidVideoTrack {
  return AndroidVideoTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidVideoTrackImpl(
    private val delegate: io.livekit.android.room.track.VideoTrack,
    private val converter: Converter,
) :
    AndroidVideoTrack,
    AndroidTrack by AndroidTrack(
        delegate = delegate,
        converter = converter,
    ) {
  override fun addRenderer(renderer: VideoSink) {
    delegate.addRenderer(renderer)
  }

  override fun removeRenderer(renderer: VideoSink) {
    delegate.removeRenderer(renderer)
  }
}
