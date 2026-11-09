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

public external interface PixelDensity : JsAny

public fun PixelDensity(value: Double): PixelDensity {
  return unsafeCast<PixelDensity>(value)
}

public object PixelDensitys {
  public val `Screen`: PixelDensity
    get() {
      return unsafeCast<PixelDensity>("screen")
    }
}
