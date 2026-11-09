package de.connect2x.livekitnity.demo.shared.permissions

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

public val LocalMediaPermissions: ProvidableCompositionLocal<MediaPermissions> =
    compositionLocalOf {
      throw IllegalStateException("No MediaPermissions object available.")
    }
