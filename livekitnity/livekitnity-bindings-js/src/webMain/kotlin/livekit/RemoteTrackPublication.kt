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

public external interface RemoteTrackPublication : TrackPublication {
  public fun `setSubscribed`(`subscribed`: Boolean): Unit

  public fun `setEnabled`(`enabled`: Boolean): Unit

  public fun `setVideoQuality`(`quality`: VideoQuality): Unit

  public fun `setVideoDimensions`(`dimensions`: TrackDimensions): Unit

  public val `videoQuality`: VideoQuality?

  public fun `setVideoFPS`(`fps`: Double): Unit

  public val `isDesired`: Boolean
  public val `subscriptionStatus`: SubscriptionStatus
  public val `permissionStatus`: PermissionStatus
}
