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

public external interface TrackSource : JsAny

public object TrackSources {
  public val `Camera`: TrackSource
    get() {
      return unsafeCast<TrackSource>("camera")
    }

  public val `Microphone`: TrackSource
    get() {
      return unsafeCast<TrackSource>("microphone")
    }

  public val `ScreenShare`: TrackSource
    get() {
      return unsafeCast<TrackSource>("screen_share")
    }

  public val `ScreenShareAudio`: TrackSource
    get() {
      return unsafeCast<TrackSource>("screen_share_audio")
    }

  public val `Unknown`: TrackSource
    get() {
      return unsafeCast<TrackSource>("unknown")
    }
}
