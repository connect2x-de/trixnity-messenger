package de.connect2x.livekitnity.compose.local

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import de.connect2x.livekitnity.compose.types.VideoTrackRenderer

/** CompositionLocal for the [VideoTrackRendererLocal] */
@Suppress("CompositionLocalNaming")
public val VideoTrackRendererLocal: ProvidableCompositionLocal<VideoTrackRenderer> =
    compositionLocalOf {
      throw IllegalStateException("No VideoTrackRenderer object available.")
    }
