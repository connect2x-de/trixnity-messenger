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

public external interface MediaDeviceKind : JsAny

public object MediaDeviceKinds {
  public val `Audioinput`: MediaDeviceKind
    get() {
      return unsafeCast<MediaDeviceKind>("audioinput")
    }

  public val `Audiooutput`: MediaDeviceKind
    get() {
      return unsafeCast<MediaDeviceKind>("audiooutput")
    }

  public val `Videoinput`: MediaDeviceKind
    get() {
      return unsafeCast<MediaDeviceKind>("videoinput")
    }
}
