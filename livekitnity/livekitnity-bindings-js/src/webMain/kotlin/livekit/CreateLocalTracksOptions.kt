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

public external interface CreateLocalTracksOptions : JsAny {
  public val `audio`: AudioCaptureChoice?
  public val `video`: VideoCaptureChoice?
}

public external interface MutableCreateLocalTracksOptions : JsAny {
  public var `audio`: AudioCaptureChoice?
  public var `video`: VideoCaptureChoice?
}

public fun CreateLocalTracksOptions(
    builder: MutableCreateLocalTracksOptions.() -> Unit,
): CreateLocalTracksOptions {
  return unsafeCast<CreateLocalTracksOptions>(
      createObject<MutableCreateLocalTracksOptions>().apply(builder),
  )
}
