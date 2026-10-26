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

public external interface AdaptiveStreamSettings : JsAny {
  public val `pixelDensity`: PixelDensity?
  public val `pauseVideoInBackground`: Boolean?
}

public external interface MutableAdaptiveStreamSettings : JsAny {
  public var `pixelDensity`: PixelDensity?
  public var `pauseVideoInBackground`: Boolean?
}

public fun AdaptiveStreamSettings(
    builder: MutableAdaptiveStreamSettings.() -> Unit,
): AdaptiveStreamSettings {
  return unsafeCast<AdaptiveStreamSettings>(
      createObject<MutableAdaptiveStreamSettings>().apply(builder),
  )
}
