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

public external interface TrackStreamState : JsAny

public object TrackStreamStates {
  public val `Active`: TrackStreamState
    get() {
      return unsafeCast<TrackStreamState>("active")
    }

  public val `Paused`: TrackStreamState
    get() {
      return unsafeCast<TrackStreamState>("paused")
    }

  public val `Unknown`: TrackStreamState
    get() {
      return unsafeCast<TrackStreamState>("unknown")
    }
}
