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

public external interface ParticipantPermissions : EventEmitter {
  public val `hidden`: Boolean
  public val `canPublish`: Boolean
  public val `canSubscribe`: Boolean
  public val `canPublishData`: Boolean
  public val `recorder`: Boolean
  public val `canPublishSources`: JsArray<ProtocolTrackSource>
  public val `canUpdateMetadata`: Boolean
  public val `canSubscribeMetrics`: Boolean
}
