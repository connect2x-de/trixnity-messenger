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

public external interface VideoTrackChoice : JsAny

public fun VideoTrackChoice(value: LocalVideoTrack): VideoTrackChoice {
  return unsafeCast<VideoTrackChoice>(value)
}

public fun VideoTrackChoice(value: RemoteVideoTrack): VideoTrackChoice {
  return unsafeCast<VideoTrackChoice>(value)
}
