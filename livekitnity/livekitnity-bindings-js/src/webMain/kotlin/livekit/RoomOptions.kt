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

public external interface RoomOptions : JsAny {
  public val `adaptiveStream`: AdaptiveStreamChoice?
  public val `dynacast`: Boolean?
  public val `audioCaptureDefaults`: AudioCaptureOptions?
  public val `videoCaptureDefaults`: VideoCaptureOptions?
  public val `stopLocalTrackOnUnpublish`: Boolean?
  public val `reconnectPolicy`: ReconnectPolicy?
  public val `disconnectOnPageLeave`: Boolean?
  public val `encryption`: E2EEOptions?
  public val `loggerName`: String?
}

public external interface MutableRoomOptions : JsAny {
  public var `adaptiveStream`: AdaptiveStreamChoice?
  public var `dynacast`: Boolean?
  public var `audioCaptureDefaults`: AudioCaptureOptions?
  public var `videoCaptureDefaults`: VideoCaptureOptions?
  public var `stopLocalTrackOnUnpublish`: Boolean?
  public var `reconnectPolicy`: ReconnectPolicy?
  public var `disconnectOnPageLeave`: Boolean?
  public var `encryption`: E2EEOptions?
  public var `loggerName`: String?
}

public fun RoomOptions(builder: MutableRoomOptions.() -> Unit): RoomOptions {
  return unsafeCast<RoomOptions>(createObject<MutableRoomOptions>().apply(builder))
}
