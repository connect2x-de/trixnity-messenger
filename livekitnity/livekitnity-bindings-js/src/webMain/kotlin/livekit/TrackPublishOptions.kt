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

public external interface TrackPublishOptions : JsAny {
  public val `name`: String?
  public val `source`: TrackSource?
  public val `stream`: String?
  public val `red`: Boolean?
  public val `simulcast`: Boolean?
  public val `dtx`: Boolean?
  public val `forceStereo`: Boolean?
  public val `stopMicTrackOnMute`: Boolean?
}

public external interface MutableTrackPublishOptions : JsAny {
  public var `name`: String?
  public var `source`: TrackSource?
  public var `stream`: String?
  public var `red`: Boolean?
  public var `simulcast`: Boolean?
  public var `dtx`: Boolean?
  public var `forceStereo`: Boolean?
  public var `stopMicTrackOnMute`: Boolean?
}

public fun TrackPublishOptions(
    builder: MutableTrackPublishOptions.() -> Unit,
): TrackPublishOptions {
  return unsafeCast<TrackPublishOptions>(createObject<MutableTrackPublishOptions>().apply(builder))
}
