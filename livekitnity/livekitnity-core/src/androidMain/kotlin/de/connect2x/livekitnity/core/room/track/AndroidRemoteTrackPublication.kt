package de.connect2x.livekitnity.core.room.track

import de.connect2x.livekitnity.api.room.track.AndroidRemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.AndroidTrackPublication
import de.connect2x.livekitnity.api.room.track.RemoteTrackPublication
import de.connect2x.livekitnity.api.room.track.Track
import de.connect2x.livekitnity.api.room.track.VideoQuality
import de.connect2x.livekitnity.core.platform.Converter

internal fun AndroidRemoteTrackPublication(
    delegate: io.livekit.android.room.track.RemoteTrackPublication,
    converter: Converter,
): AndroidRemoteTrackPublication {
  return AndroidRemoteTrackPublicationImpl(delegate, converter)
}

private class AndroidRemoteTrackPublicationImpl(
    private val delegate: io.livekit.android.room.track.RemoteTrackPublication,
    converter: Converter,
) :
    AndroidRemoteTrackPublication,
    AndroidTrackPublication by AndroidTrackPublication(delegate, converter) {

  override val isDesired: Boolean
    get() = delegate.isDesired

  override val subscriptionAllowed: Boolean
    get() = delegate.subscriptionAllowed

  override val subscriptionStatus: RemoteTrackPublication.SubscriptionStatus
    get() = Converter.toCommon(delegate.subscriptionStatus)

  override fun setSubscribed(subscribed: Boolean) {
    delegate.setSubscribed(subscribed)
  }

  override fun setEnabled(enabled: Boolean) {
    delegate.setEnabled(enabled)
  }

  override fun setVideoQuality(quality: VideoQuality) {
    delegate.setVideoQuality(Converter.fromCommon(quality))
  }

  override fun setVideoDimensions(dimensions: Track.Dimensions) {
    delegate.setVideoDimensions(Converter.fromCommon(dimensions))
  }

  override fun setVideoFps(fps: Int) {
    delegate.setVideoFps(fps)
  }
}
