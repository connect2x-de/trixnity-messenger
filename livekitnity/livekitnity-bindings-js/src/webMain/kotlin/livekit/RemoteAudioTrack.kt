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

public external interface RemoteAudioTrack : RemoteTrack {
  public fun `setVolume`(`volume`: Double): Unit

  public fun `getVolume`(): Double

  public fun `setSinkId`(`deviceId`: String): Promise<Nothing?>
}
