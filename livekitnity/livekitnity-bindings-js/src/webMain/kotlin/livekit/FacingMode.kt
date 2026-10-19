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

public external interface FacingMode : JsAny

public object FacingModes {
  public val `Left`: FacingMode
    get() {
      return unsafeCast<FacingMode>("left")
    }

  public val `Right`: FacingMode
    get() {
      return unsafeCast<FacingMode>("right")
    }

  public val `Environment`: FacingMode
    get() {
      return unsafeCast<FacingMode>("environment")
    }

  public val `User`: FacingMode
    get() {
      return unsafeCast<FacingMode>("user")
    }
}
