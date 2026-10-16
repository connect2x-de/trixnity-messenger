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

public external interface PublishableTrack : JsAny

public fun PublishableTrack(value: org.w3c.dom.mediacapture.MediaStreamTrack): PublishableTrack {
  return unsafeCast<PublishableTrack>(value)
}

public fun PublishableTrack(value: LocalTrack): PublishableTrack {
  return unsafeCast<PublishableTrack>(value)
}
