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

public external interface AudioCaptureChoice : JsAny

public fun AudioCaptureChoice(value: Boolean): AudioCaptureChoice {
  return unsafeCast<AudioCaptureChoice>(value)
}

public fun AudioCaptureChoice(value: AudioCaptureOptions): AudioCaptureChoice {
  return unsafeCast<AudioCaptureChoice>(value)
}
