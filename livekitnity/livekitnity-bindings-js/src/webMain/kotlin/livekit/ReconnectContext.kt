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

public external interface ReconnectContext : JsAny {
  public val `elapsedMs`: Double
  public val `retryCount`: Double
}

public external interface MutableReconnectContext : JsAny {
  public var `elapsedMs`: Double
  public var `retryCount`: Double
}

public fun ReconnectContext(builder: MutableReconnectContext.() -> Unit): ReconnectContext {
  return unsafeCast<ReconnectContext>(createObject<MutableReconnectContext>().apply(builder))
}
