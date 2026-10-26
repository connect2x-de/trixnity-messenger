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

public external interface E2EEManagerOptions : JsAny {
  public val `worker`: org.w3c.dom.Worker
  public val `keyProvider`: BaseKeyProvider
}

public external interface MutableE2EEManagerOptions : JsAny {
  public var `worker`: org.w3c.dom.Worker
  public var `keyProvider`: BaseKeyProvider
}

public fun E2EEManagerOptions(builder: MutableE2EEManagerOptions.() -> Unit): E2EEManagerOptions {
  return unsafeCast<E2EEManagerOptions>(createObject<MutableE2EEManagerOptions>().apply(builder))
}
