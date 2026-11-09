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

public external interface AudioTrackSource : JsAny

public object AudioTrackSources {
  public val `Microphone`: AudioTrackSource
    get() {
      return unsafeCast<AudioTrackSource>("microphone")
    }

  public val `ScreenShareAudio`: AudioTrackSource
    get() {
      return unsafeCast<AudioTrackSource>("screen_share_audio")
    }
}
