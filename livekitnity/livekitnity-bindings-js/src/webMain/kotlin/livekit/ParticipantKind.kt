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

public external interface ParticipantKind : JsAny

public object ParticipantKinds {
  public val `STANDARD`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(0.0)
    }

  public val `INGRESS`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(1.0)
    }

  public val `EGRESS`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(2.0)
    }

  public val `SIP`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(3.0)
    }

  public val `AGENT`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(4.0)
    }

  public val `CONNECTOR`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(7.0)
    }

  public val `BRIDGE`: ParticipantKind
    get() {
      return unsafeCast<ParticipantKind>(8.0)
    }
}
