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

public external interface ConnectionState : JsAny

public object ConnectionStates {
  public val `Disconnected`: ConnectionState
    get() {
      return unsafeCast<ConnectionState>("disconnected")
    }

  public val `Connecting`: ConnectionState
    get() {
      return unsafeCast<ConnectionState>("connecting")
    }

  public val `Connected`: ConnectionState
    get() {
      return unsafeCast<ConnectionState>("connected")
    }

  public val `Reconnecting`: ConnectionState
    get() {
      return unsafeCast<ConnectionState>("reconnecting")
    }

  public val `SignalReconnecting`: ConnectionState
    get() {
      return unsafeCast<ConnectionState>("signalReconnecting")
    }
}
