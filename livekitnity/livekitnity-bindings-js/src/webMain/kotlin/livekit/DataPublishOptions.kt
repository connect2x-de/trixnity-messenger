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

public external interface DataPublishOptions : JsAny {
  public val `topic`: String?
  public val `destinationIdentities`: JsArray<JsString>?
  public val `reliable`: Boolean?
}

public external interface MutableDataPublishOptions : JsAny {
  public var `topic`: String?
  public var `destinationIdentities`: JsArray<JsString>?
  public var `reliable`: Boolean?
}

public fun DataPublishOptions(builder: MutableDataPublishOptions.() -> Unit): DataPublishOptions {
  return unsafeCast<DataPublishOptions>(createObject<MutableDataPublishOptions>().apply(builder))
}
