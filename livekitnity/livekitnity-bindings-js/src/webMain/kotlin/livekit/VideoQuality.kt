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

public external interface VideoQuality : JsAny

public object VideoQualitys {
  public val `LOW`: VideoQuality
    get() {
      return unsafeCast<VideoQuality>(0.0)
    }

  public val `MEDIUM`: VideoQuality
    get() {
      return unsafeCast<VideoQuality>(1.0)
    }

  public val `HIGH`: VideoQuality
    get() {
      return unsafeCast<VideoQuality>(2.0)
    }
}
