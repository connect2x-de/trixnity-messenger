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

public external interface ManagedE2EEOptions : JsAny {}

public external interface MutableManagedE2EEOptions : JsAny {}

public fun ManagedE2EEOptions(builder: MutableManagedE2EEOptions.() -> Unit): ManagedE2EEOptions {
  return unsafeCast<ManagedE2EEOptions>(createObject<MutableManagedE2EEOptions>().apply(builder))
}
