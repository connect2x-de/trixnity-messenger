package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.RemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.VideoQuality
import de.connect2x.livekitnity.api.room.track.WebRemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.WebTrackPublication
import de.connect2x.livekitnity.core.platform.Converter

internal fun WebRemoteTrackPublication(
    remoteTrackPublication: livekit.RemoteTrackPublication,
    converter: Converter,
): WebRemoteTrackPublication {
  return WebRemoteTrackPublicationImpl(
      remoteTrackPublication = remoteTrackPublication,
      converter = converter,
  )
}

private class WebRemoteTrackPublicationImpl
private constructor(
    private val remoteTrackPublication: livekit.RemoteTrackPublication,
    private val base: WebTrackPublication,
) : WebRemoteTrackPublication, WebTrackPublication by base {

  constructor(
      remoteTrackPublication: livekit.RemoteTrackPublication,
      converter: Converter,
  ) : this(
      remoteTrackPublication = remoteTrackPublication,
      base =
          WebTrackPublication(
              trackPublication = remoteTrackPublication,
              converter = converter,
          ),
  )

  override val isDesired: Boolean
    get() = remoteTrackPublication.isDesired

  override val subscriptionAllowed: Boolean
    get() = remoteTrackPublication.permissionStatus == livekit.PermissionStatuss.Allowed

  override val subscriptionStatus: RemoteTrackPublication.SubscriptionStatus
    get() = Converter.toCommon(remoteTrackPublication.subscriptionStatus)

  override fun setSubscribed(subscribed: Boolean) {
    remoteTrackPublication.setSubscribed(subscribed)
  }

  override fun setEnabled(enabled: Boolean) {
    remoteTrackPublication.setEnabled(enabled)
  }

  override fun setVideoQuality(quality: VideoQuality) {
    remoteTrackPublication.setVideoQuality(
        quality = Converter.fromCommon(quality),
    )
  }

  override fun setVideoDimensions(dimensions: Track.Dimensions) {
    remoteTrackPublication.setVideoDimensions(
        dimensions = Converter.fromCommon(dimensions),
    )
  }

  override fun setVideoFps(fps: Int) {
    remoteTrackPublication.setVideoFPS(fps.toDouble())
  }
}
