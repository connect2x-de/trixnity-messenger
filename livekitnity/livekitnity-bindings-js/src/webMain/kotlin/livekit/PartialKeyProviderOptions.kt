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

public external interface PartialKeyProviderOptions : JsAny {
  public val `sharedKey`: Boolean?
  public val `ratchetSalt`: String?
  public val `ratchetWindowSize`: Double?
  public val `failureTolerance`: Double?
  public val `keyringSize`: Double?
  public val `keySize`: KeySize?
}

public external interface MutablePartialKeyProviderOptions : JsAny {
  public var `sharedKey`: Boolean?
  public var `ratchetSalt`: String?
  public var `ratchetWindowSize`: Double?
  public var `failureTolerance`: Double?
  public var `keyringSize`: Double?
  public var `keySize`: KeySize?
}

public fun PartialKeyProviderOptions(
    builder: MutablePartialKeyProviderOptions.() -> Unit,
): PartialKeyProviderOptions {
  return unsafeCast<PartialKeyProviderOptions>(
      createObject<MutablePartialKeyProviderOptions>().apply(builder),
  )
}
