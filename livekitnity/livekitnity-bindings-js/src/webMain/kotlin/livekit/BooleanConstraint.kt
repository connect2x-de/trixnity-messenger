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

public external interface BooleanConstraint : JsAny

public fun BooleanConstraint(value: Boolean): BooleanConstraint {
  return unsafeCast<BooleanConstraint>(value)
}

public fun BooleanConstraint(
    value: org.w3c.dom.mediacapture.ConstrainBooleanParameters,
): BooleanConstraint {
  return unsafeCast<BooleanConstraint>(value)
}
