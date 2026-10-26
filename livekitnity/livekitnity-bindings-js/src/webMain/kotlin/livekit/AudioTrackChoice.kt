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

public external interface AudioTrackChoice : JsAny

public fun AudioTrackChoice(value: LocalAudioTrack): AudioTrackChoice {
  return unsafeCast<AudioTrackChoice>(value)
}

public fun AudioTrackChoice(value: RemoteAudioTrack): AudioTrackChoice {
  return unsafeCast<AudioTrackChoice>(value)
}
