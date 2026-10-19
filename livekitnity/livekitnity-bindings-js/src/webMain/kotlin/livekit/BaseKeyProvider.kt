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

public external interface BaseKeyProvider : EventEmitter {}

@JsModule("livekit-client")
private external object BaseKeyProviderModule {
  val `BaseKeyProvider`: Constructor<BaseKeyProvider>
}

public fun BaseKeyProvider(`options`: PartialKeyProviderOptions? = undefined()): BaseKeyProvider {
  return construct(BaseKeyProviderModule.`BaseKeyProvider`, options)
}
