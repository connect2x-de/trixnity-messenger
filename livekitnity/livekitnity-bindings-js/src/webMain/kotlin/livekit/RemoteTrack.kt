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

public external interface RemoteTrack : Track {
  public fun `start`(): Unit

  public fun `setPlayoutDelay`(`delayInSeconds`: Double): Unit

  public fun `getPlayoutDelay`(): Double
}
