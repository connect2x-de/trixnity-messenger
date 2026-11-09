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

public external interface TrackKind : JsAny

public object TrackKinds {
  public val `Audio`: TrackKind
    get() {
      return unsafeCast<TrackKind>("audio")
    }

  public val `Video`: TrackKind
    get() {
      return unsafeCast<TrackKind>("video")
    }

  public val `Unknown`: TrackKind
    get() {
      return unsafeCast<TrackKind>("unknown")
    }
}
