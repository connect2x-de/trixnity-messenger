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

public external interface NumberConstraint : JsAny

public fun NumberConstraint(value: Double): NumberConstraint {
  return unsafeCast<NumberConstraint>(value)
}

public fun NumberConstraint(
    value: org.w3c.dom.mediacapture.ConstrainDoubleRange,
): NumberConstraint {
  return unsafeCast<NumberConstraint>(value)
}
