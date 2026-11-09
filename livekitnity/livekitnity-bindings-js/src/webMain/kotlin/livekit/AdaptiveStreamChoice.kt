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

public external interface AdaptiveStreamChoice : JsAny

public fun AdaptiveStreamChoice(value: Boolean): AdaptiveStreamChoice {
  return unsafeCast<AdaptiveStreamChoice>(value)
}

public fun AdaptiveStreamChoice(value: AdaptiveStreamSettings): AdaptiveStreamChoice {
  return unsafeCast<AdaptiveStreamChoice>(value)
}
