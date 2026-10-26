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

public external interface KeySize : JsAny

public object KeySizes {
  public val `128`: KeySize
    get() {
      return unsafeCast<KeySize>(128.0)
    }

  public val `256`: KeySize
    get() {
      return unsafeCast<KeySize>(256.0)
    }
}
