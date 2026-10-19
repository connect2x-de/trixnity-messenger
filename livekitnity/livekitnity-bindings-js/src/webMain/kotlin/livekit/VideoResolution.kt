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

public external interface VideoResolution : JsAny {
  public val `height`: Double
  public val `width`: Double
  public val `aspectRatio`: Double?
  public val `frameRate`: Double?
}

public external interface MutableVideoResolution : JsAny {
  public var `height`: Double
  public var `width`: Double
  public var `aspectRatio`: Double?
  public var `frameRate`: Double?
}

public fun VideoResolution(builder: MutableVideoResolution.() -> Unit): VideoResolution {
  return unsafeCast<VideoResolution>(createObject<MutableVideoResolution>().apply(builder))
}
