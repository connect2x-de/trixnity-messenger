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

public external interface SubscriptionError : JsAny

public object SubscriptionErrors {
  public val `SE_UNKNOWN`: SubscriptionError
    get() {
      return unsafeCast<SubscriptionError>(0.0)
    }

  public val `SE_CODEC_UNSUPPORTED`: SubscriptionError
    get() {
      return unsafeCast<SubscriptionError>(1.0)
    }

  public val `SE_TRACK_NOTFOUND`: SubscriptionError
    get() {
      return unsafeCast<SubscriptionError>(2.0)
    }
}
