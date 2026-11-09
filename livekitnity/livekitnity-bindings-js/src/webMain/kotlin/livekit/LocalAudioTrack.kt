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

public external interface LocalAudioTrack : LocalTrack {
  public fun `restartTrack`(`options`: AudioCaptureOptions? = definedExternally): Promise<Nothing?>

  public fun `checkForSilence`(): Promise<JsBoolean>

  public val `stopOnMute`: Boolean
  public val `enhancedNoiseCancellation`: Boolean
}
