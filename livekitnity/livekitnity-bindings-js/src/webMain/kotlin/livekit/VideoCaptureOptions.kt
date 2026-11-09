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

public external interface VideoCaptureOptions : JsAny {
  public val `resolution`: VideoResolution?
  public val `deviceId`: DeviceConstraint?
  public val `facingMode`: FacingMode?
  public val `frameRate`: NumberConstraint?
}

public external interface MutableVideoCaptureOptions : JsAny {
  public var `resolution`: VideoResolution?
  public var `deviceId`: DeviceConstraint?
  public var `facingMode`: FacingMode?
  public var `frameRate`: NumberConstraint?
}

public fun VideoCaptureOptions(
    builder: MutableVideoCaptureOptions.() -> Unit,
): VideoCaptureOptions {
  return unsafeCast<VideoCaptureOptions>(createObject<MutableVideoCaptureOptions>().apply(builder))
}
