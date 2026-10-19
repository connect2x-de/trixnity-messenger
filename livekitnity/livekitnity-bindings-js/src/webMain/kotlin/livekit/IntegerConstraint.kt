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

public external interface IntegerConstraint : JsAny

public fun IntegerConstraint(value: Double): IntegerConstraint {
  return unsafeCast<IntegerConstraint>(value)
}

public fun IntegerConstraint(
    value: org.w3c.dom.mediacapture.ConstrainULongRange,
): IntegerConstraint {
  return unsafeCast<IntegerConstraint>(value)
}
