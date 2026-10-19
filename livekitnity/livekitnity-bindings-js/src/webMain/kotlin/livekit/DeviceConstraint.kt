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

public external interface DeviceConstraint : JsAny

public fun DeviceConstraint(value: String): DeviceConstraint {
  return unsafeCast<DeviceConstraint>(value)
}

public fun DeviceConstraint(value: JsArray<JsString>): DeviceConstraint {
  return unsafeCast<DeviceConstraint>(value)
}

public fun DeviceConstraint(
    value: org.w3c.dom.mediacapture.ConstrainDOMStringParameters,
): DeviceConstraint {
  return unsafeCast<DeviceConstraint>(value)
}
