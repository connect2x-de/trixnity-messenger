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

public external interface LocalVideoTrack : LocalTrack {
  public fun `restartTrack`(`options`: VideoCaptureOptions? = definedExternally): Promise<Nothing?>

  public val `isSimulcast`: Boolean

  public fun `setPublishingQuality`(`maxQuality`: VideoQuality): Unit
}
