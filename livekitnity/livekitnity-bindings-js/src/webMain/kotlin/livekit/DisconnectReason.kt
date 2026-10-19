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

public external interface DisconnectReason : JsAny

public object DisconnectReasons {
  public val `UNKNOWN_REASON`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(0.0)
    }

  public val `CLIENT_INITIATED`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(1.0)
    }

  public val `DUPLICATE_IDENTITY`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(2.0)
    }

  public val `SERVER_SHUTDOWN`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(3.0)
    }

  public val `PARTICIPANT_REMOVED`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(4.0)
    }

  public val `ROOM_DELETED`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(5.0)
    }

  public val `STATE_MISMATCH`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(6.0)
    }

  public val `JOIN_FAILURE`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(7.0)
    }

  public val `MIGRATION`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(8.0)
    }

  public val `SIGNAL_CLOSE`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(9.0)
    }

  public val `ROOM_CLOSED`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(10.0)
    }

  public val `USER_UNAVAILABLE`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(11.0)
    }

  public val `USER_REJECTED`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(12.0)
    }

  public val `SIP_TRUNK_FAILURE`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(13.0)
    }

  public val `CONNECTION_TIMEOUT`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(14.0)
    }

  public val `MEDIA_FAILURE`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(15.0)
    }

  public val `AGENT_ERROR`: DisconnectReason
    get() {
      return unsafeCast<DisconnectReason>(16.0)
    }
}
