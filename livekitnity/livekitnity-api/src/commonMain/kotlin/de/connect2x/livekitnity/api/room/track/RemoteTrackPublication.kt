package de.connect2x.livekitnity.api.room.track

public interface RemoteTrackPublication : TrackPublication {
  public val isDesired: Boolean

  public val subscriptionAllowed: Boolean

  public val subscriptionStatus: SubscriptionStatus

  public fun setSubscribed(subscribed: Boolean)

  public fun setEnabled(enabled: Boolean)

  public fun setVideoQuality(quality: VideoQuality)

  public fun setVideoDimensions(dimensions: Track.Dimensions)

  public fun setVideoFps(fps: Int)

  public enum class SubscriptionStatus {
    SUBSCRIBED,
    SUBSCRIBED_AND_NOT_ALLOWED,
    UNSUBSCRIBED,
  }
}
