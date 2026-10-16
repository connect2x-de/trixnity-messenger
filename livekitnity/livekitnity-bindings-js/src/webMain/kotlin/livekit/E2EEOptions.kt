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

public external interface E2EEOptions : JsAny

public fun E2EEOptions(value: E2EEManagerOptions): E2EEOptions {
  return unsafeCast<E2EEOptions>(value)
}

public fun E2EEOptions(value: ManagedE2EEOptions): E2EEOptions {
  return unsafeCast<E2EEOptions>(value)
}
