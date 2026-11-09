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

public external interface ReconnectPolicy : JsAny {
  public fun `nextRetryDelayInMs`(`context`: ReconnectContext): Double?
}

public external interface MutableReconnectPolicy : JsAny {
  public var `nextRetryDelayInMs`: (ReconnectContext) -> Double?
}

public fun ReconnectPolicy(builder: MutableReconnectPolicy.() -> Unit): ReconnectPolicy {
  return unsafeCast<ReconnectPolicy>(createObject<MutableReconnectPolicy>().apply(builder))
}
