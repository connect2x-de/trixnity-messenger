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

public external interface InternalRoomOptions : JsAny {
  public val `encryption`: E2EEOptions?
}

public external interface MutableInternalRoomOptions : JsAny {
  public var `encryption`: E2EEOptions?
}

public fun InternalRoomOptions(
    builder: MutableInternalRoomOptions.() -> Unit,
): InternalRoomOptions {
  return unsafeCast<InternalRoomOptions>(createObject<MutableInternalRoomOptions>().apply(builder))
}
