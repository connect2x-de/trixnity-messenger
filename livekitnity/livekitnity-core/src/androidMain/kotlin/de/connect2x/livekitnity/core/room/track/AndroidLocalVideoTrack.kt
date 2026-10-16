package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidLocalVideoTrack
import de.connect2x.livekitnity.api.room.track.AndroidVideoTrack
import de.connect2x.livekitnity.api.room.track.CameraPosition
import de.connect2x.livekitnity.core.platform.Converter

internal fun AndroidLocalVideoTrack(
    delegate: io.livekit.android.room.track.LocalVideoTrack,
    converter: Converter,
): AndroidLocalVideoTrack {
  return AndroidLocalVideoTrackImpl(
      delegate = delegate,
      converter = converter,
  )
}

private class AndroidLocalVideoTrackImpl(
    private val delegate: io.livekit.android.room.track.LocalVideoTrack,
    converter: Converter,
) :
    AndroidLocalVideoTrack,
    AndroidVideoTrack by AndroidVideoTrack(
        delegate = delegate,
        converter = converter,
    ) {

  override val deviceId: String?
    get() = delegate.options.deviceId

  override val position: CameraPosition?
    get() = Converter.toCommon(delegate.options.position ?: return null)

  override fun switchCamera(
      deviceId: String?,
      position: CameraPosition?,
  ) {
    delegate.switchCamera(
        deviceId = deviceId,
        position = position?.let { Converter.fromCommon(it) },
    )
  }
}
