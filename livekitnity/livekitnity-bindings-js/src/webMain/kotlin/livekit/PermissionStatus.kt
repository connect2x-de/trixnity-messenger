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

public external interface PermissionStatus : JsAny

public object PermissionStatuss {
  public val `Allowed`: PermissionStatus
    get() {
      return unsafeCast<PermissionStatus>("allowed")
    }

  public val `NotAllowed`: PermissionStatus
    get() {
      return unsafeCast<PermissionStatus>("not_allowed")
    }
}
