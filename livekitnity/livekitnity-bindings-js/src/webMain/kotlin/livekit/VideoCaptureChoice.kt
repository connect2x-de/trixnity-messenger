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

public external interface VideoCaptureChoice : JsAny

public fun VideoCaptureChoice(value: Boolean): VideoCaptureChoice {
  return unsafeCast<VideoCaptureChoice>(value)
}

public fun VideoCaptureChoice(value: VideoCaptureOptions): VideoCaptureChoice {
  return unsafeCast<VideoCaptureChoice>(value)
}
