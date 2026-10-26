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

public external interface SubscriptionStatus : JsAny

public object SubscriptionStatuss {
  public val `Desired`: SubscriptionStatus
    get() {
      return unsafeCast<SubscriptionStatus>("desired")
    }

  public val `Subscribed`: SubscriptionStatus
    get() {
      return unsafeCast<SubscriptionStatus>("subscribed")
    }

  public val `Unsubscribed`: SubscriptionStatus
    get() {
      return unsafeCast<SubscriptionStatus>("unsubscribed")
    }
}
