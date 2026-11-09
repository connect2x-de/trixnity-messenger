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

public external interface SurfaceChoice : JsAny

public object SurfaceChoices {
  public val `Include`: SurfaceChoice
    get() {
      return unsafeCast<SurfaceChoice>("include")
    }

  public val `Exclude`: SurfaceChoice
    get() {
      return unsafeCast<SurfaceChoice>("exclude")
    }
}
