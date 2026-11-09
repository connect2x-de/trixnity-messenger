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

public external interface ConnectionQuality : JsAny

public object ConnectionQualitys {
  public val `Excellent`: ConnectionQuality
    get() {
      return unsafeCast<ConnectionQuality>("excellent")
    }

  public val `Good`: ConnectionQuality
    get() {
      return unsafeCast<ConnectionQuality>("good")
    }

  public val `Poor`: ConnectionQuality
    get() {
      return unsafeCast<ConnectionQuality>("poor")
    }

  public val `Lost`: ConnectionQuality
    get() {
      return unsafeCast<ConnectionQuality>("lost")
    }

  public val `Unknown`: ConnectionQuality
    get() {
      return unsafeCast<ConnectionQuality>("unknown")
    }
}
