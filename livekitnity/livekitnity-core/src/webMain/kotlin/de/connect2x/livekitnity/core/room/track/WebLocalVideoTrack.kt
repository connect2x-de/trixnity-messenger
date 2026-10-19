package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.CameraPosition
import de.connect2x.livekitnity.api.room.track.WebLocalVideoTrack
import de.connect2x.livekitnity.api.room.track.WebTrack
import de.connect2x.livekitnity.core.platform.Converter
import org.w3c.dom.HTMLVideoElement

internal fun WebLocalVideoTrack(
    localVideoTrack: livekit.LocalVideoTrack,
    converter: Converter,
    trackName: String,
): WebLocalVideoTrack {
  return WebLocalVideoTrackImpl(
      localVideoTrack = localVideoTrack,
      converter = converter,
      trackName = trackName,
  )
}

private class WebLocalVideoTrackImpl
private constructor(
    private val localVideoTrack: livekit.LocalVideoTrack,
    private val base: WebTrack,
) : WebLocalVideoTrack, WebTrack by base {

  constructor(
      localVideoTrack: livekit.LocalVideoTrack,
      converter: Converter,
      trackName: String,
  ) : this(
      localVideoTrack = localVideoTrack,
      base =
          WebTrack(
              track = localVideoTrack,
              converter = converter,
              trackName = trackName,
          ),
  )

  override val deviceId: String?
    get() = localVideoTrack.mediaStreamTrack.getSettings().deviceId

  override val position: CameraPosition?
    get() =
        Converter.toCameraPosition(
            localVideoTrack.mediaStreamTrack.getSettings().facingMode ?: return null,
        )

  override fun attach(element: HTMLVideoElement) {
    localVideoTrack.attach(element)
  }

  override fun detach(element: HTMLVideoElement) {
    localVideoTrack.detach(element)
  }

  // TODO(fhilgers): implement switchCamera
  override fun switchCamera(
      deviceId: String?,
      position: CameraPosition?,
  ) {
    println("An operation is not implemented: WebLocalVideoTrack.switchCamera")
  }
}
