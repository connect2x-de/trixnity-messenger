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

public external interface ParticipantChoice : JsAny

public fun ParticipantChoice(value: RemoteParticipant): ParticipantChoice {
  return unsafeCast<ParticipantChoice>(value)
}

public fun ParticipantChoice(value: LocalParticipant): ParticipantChoice {
  return unsafeCast<ParticipantChoice>(value)
}
