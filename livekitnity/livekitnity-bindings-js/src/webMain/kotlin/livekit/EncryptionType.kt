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

public external interface EncryptionType : JsAny

public object EncryptionTypes {
  public val `NONE`: EncryptionType
    get() {
      return unsafeCast<EncryptionType>(0.0)
    }

  public val `GCM`: EncryptionType
    get() {
      return unsafeCast<EncryptionType>(1.0)
    }

  public val `CUSTOM`: EncryptionType
    get() {
      return unsafeCast<EncryptionType>(2.0)
    }
}
