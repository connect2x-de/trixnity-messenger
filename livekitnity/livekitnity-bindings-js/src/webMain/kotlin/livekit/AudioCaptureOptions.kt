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

public external interface AudioCaptureOptions : JsAny {
  public val `autoGainControl`: BooleanConstraint?
  public val `noiseSuppression`: BooleanConstraint?
  public val `echoCancellation`: BooleanConstraint?
  public val `channelCount`: IntegerConstraint?
  public val `deviceId`: DeviceConstraint?
  public val `sampleRate`: IntegerConstraint?
}

public external interface MutableAudioCaptureOptions : JsAny {
  public var `autoGainControl`: BooleanConstraint?
  public var `noiseSuppression`: BooleanConstraint?
  public var `echoCancellation`: BooleanConstraint?
  public var `channelCount`: IntegerConstraint?
  public var `deviceId`: DeviceConstraint?
  public var `sampleRate`: IntegerConstraint?
}

public fun AudioCaptureOptions(
    builder: MutableAudioCaptureOptions.() -> Unit,
): AudioCaptureOptions {
  return unsafeCast<AudioCaptureOptions>(createObject<MutableAudioCaptureOptions>().apply(builder))
}
