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

public external interface TrackDimensions : JsAny {
  public val `height`: Double
  public val `width`: Double
}

public external interface MutableTrackDimensions : JsAny {
  public var `height`: Double
  public var `width`: Double
}

public fun TrackDimensions(builder: MutableTrackDimensions.() -> Unit): TrackDimensions {
  return unsafeCast<TrackDimensions>(createObject<MutableTrackDimensions>().apply(builder))
}
