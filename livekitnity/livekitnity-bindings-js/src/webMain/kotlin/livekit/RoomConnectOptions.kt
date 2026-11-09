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

public external interface RoomConnectOptions : JsAny {
  public val `autoSubscribe`: Boolean?
  public val `peerConnectionTimeout`: Double?
  public val `maxRetries`: Double?
  public val `websocketTimeout`: Double?
}

public external interface MutableRoomConnectOptions : JsAny {
  public var `autoSubscribe`: Boolean?
  public var `peerConnectionTimeout`: Double?
  public var `maxRetries`: Double?
  public var `websocketTimeout`: Double?
}

public fun RoomConnectOptions(builder: MutableRoomConnectOptions.() -> Unit): RoomConnectOptions {
  return unsafeCast<RoomConnectOptions>(createObject<MutableRoomConnectOptions>().apply(builder))
}
