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

public external interface DataPacketKind : JsAny

public object DataPacketKinds {
  public val `RELIABLE`: DataPacketKind
    get() {
      return unsafeCast<DataPacketKind>(0.0)
    }

  public val `LOSSY`: DataPacketKind
    get() {
      return unsafeCast<DataPacketKind>(1.0)
    }
}
