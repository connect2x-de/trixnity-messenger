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

public external interface RoomModel : JsAny {
  public val `sid`: String
}

public external interface MutableRoomModel : JsAny {
  public var `sid`: String
}

public fun RoomModel(builder: MutableRoomModel.() -> Unit): RoomModel {
  return unsafeCast<RoomModel>(createObject<MutableRoomModel>().apply(builder))
}
