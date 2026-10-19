package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.WebRemoteVideoTrack
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.core.platform.Converter
import org.w3c.dom.HTMLVideoElement

internal fun WebRemoteVideoTrack(
    remoteVideoTrack: livekit.RemoteVideoTrack,
    converter: Converter,
    trackName: String,
): WebRemoteVideoTrack {
  return WebRemoteVideoTrackImpl(
      remoteVideoTrack = remoteVideoTrack,
      converter = converter,
      trackName = trackName,
  )
}

private class WebRemoteVideoTrackImpl
private constructor(
    private val remoteVideoTrack: livekit.RemoteVideoTrack,
    private val base: WebTrack,
) : WebRemoteVideoTrack, WebTrack by base {

  constructor(
      remoteVideoTrack: livekit.RemoteVideoTrack,
      converter: Converter,
      trackName: String,
  ) : this(
      remoteVideoTrack = remoteVideoTrack,
      base =
          WebTrack(
              track = remoteVideoTrack,
              converter = converter,
              trackName = trackName,
          ),
  )

  override fun attach(element: HTMLVideoElement) {
    remoteVideoTrack.attach(element)
  }

  override fun detach(element: HTMLVideoElement) {
    remoteVideoTrack.detach(element)
  }
}
