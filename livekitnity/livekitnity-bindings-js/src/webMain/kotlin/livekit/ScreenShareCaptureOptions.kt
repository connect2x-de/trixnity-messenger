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

public external interface ScreenShareCaptureOptions : JsAny {
  public val `resolution`: VideoResolution?
  public val `audio`: AudioCaptureChoice?
  public val `systemAudio`: SurfaceChoice?
  public val `selfBrowserSurface`: SurfaceChoice?
  public val `surfaceSwitching`: SurfaceChoice?
  public val `preferCurrentTab`: Boolean?
}

public external interface MutableScreenShareCaptureOptions : JsAny {
  public var `resolution`: VideoResolution?
  public var `audio`: AudioCaptureChoice?
  public var `systemAudio`: SurfaceChoice?
  public var `selfBrowserSurface`: SurfaceChoice?
  public var `surfaceSwitching`: SurfaceChoice?
  public var `preferCurrentTab`: Boolean?
}

public fun ScreenShareCaptureOptions(
    builder: MutableScreenShareCaptureOptions.() -> Unit,
): ScreenShareCaptureOptions {
  return unsafeCast<ScreenShareCaptureOptions>(
      createObject<MutableScreenShareCaptureOptions>().apply(builder),
  )
}
