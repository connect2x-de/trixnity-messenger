package de.connect2x.livekitnity.compose.local

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import de.connect2x.livekitnity.api.LiveKit

/** CompositionLocal for the [LiveKit] */
@Suppress("CompositionLocalNaming")
public val LiveKitLocal: ProvidableCompositionLocal<LiveKit> = compositionLocalOf {
  throw IllegalStateException("No LiveKit object available.")
}
