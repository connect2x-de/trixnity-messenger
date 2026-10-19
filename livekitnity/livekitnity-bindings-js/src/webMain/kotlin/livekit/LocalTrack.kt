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

public external interface LocalTrack : Track {
  public fun `mute`(): Promise<LocalTrack>

  public fun `unmute`(): Promise<LocalTrack>

  public fun `pauseUpstream`(): Promise<Nothing?>

  public fun `resumeUpstream`(): Promise<Nothing?>

  public val `isUpstreamPaused`: Boolean
}
