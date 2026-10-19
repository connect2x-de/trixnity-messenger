@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress(
    // "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "RemoveRedundantBackticks",
    "RemoveExplicitTypeArguments",
    "RedundantUnitReturnType",
    "UnnecessaryOptInAnnotation",
    // "unused",
)

package livekit

import kotlin.js.*

public external interface TrackPublication : EventEmitter {
  public val `source`: TrackSource
  public val `track`: Track?
  public val `kind`: TrackKind
  public val `mimeType`: String?
  public val `isLocal`: Boolean
  public val `isMuted`: Boolean
  public val `dimensions`: TrackDimensions?
  public val `trackSid`: String
  public val `trackName`: String
  public val `simulcasted`: Boolean?
  public val `isEnabled`: Boolean
  public val `isSubscribed`: Boolean
  public val `audioTrack`: AudioTrackChoice?
  public val `videoTrack`: VideoTrackChoice?
}

public object TrackPublicationEvent {
  public val Ended: EventCallback1<Track?> = eventCallback1("ended")
  public val Muted: EventCallback0 = eventCallback0("muted")
  public val Unmuted: EventCallback0 = eventCallback0("unmuted")
  public val SubscriptionPermissionChanged: EventCallback2<PermissionStatus, PermissionStatus> =
      eventCallback2("subscriptionPermissionChanged")
  public val Subscribed: EventCallback1<RemoteTrack> = eventCallback1("subscribed")
  public val Unsubscribed: EventCallback1<RemoteTrack> = eventCallback1("unsubscribed")
  public val SubscriptionStatusChanged: EventCallback2<SubscriptionStatus, SubscriptionStatus> =
      eventCallback2("subscriptionStatusChanged")
  public val SubscriptionFailed: EventCallback1<SubscriptionError> =
      eventCallback1("subscriptionFailed")
}
