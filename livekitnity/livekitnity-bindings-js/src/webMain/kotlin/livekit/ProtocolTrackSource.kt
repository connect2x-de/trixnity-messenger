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

public external interface ProtocolTrackSource : JsAny

public object ProtocolTrackSources {
  public val `UNKNOWN`: ProtocolTrackSource
    get() {
      return unsafeCast<ProtocolTrackSource>(0.0)
    }

  public val `CAMERA`: ProtocolTrackSource
    get() {
      return unsafeCast<ProtocolTrackSource>(1.0)
    }

  public val `MICROPHONE`: ProtocolTrackSource
    get() {
      return unsafeCast<ProtocolTrackSource>(2.0)
    }

  public val `SCREEN_SHARE`: ProtocolTrackSource
    get() {
      return unsafeCast<ProtocolTrackSource>(3.0)
    }

  public val `SCREEN_SHARE_AUDIO`: ProtocolTrackSource
    get() {
      return unsafeCast<ProtocolTrackSource>(4.0)
    }
}
