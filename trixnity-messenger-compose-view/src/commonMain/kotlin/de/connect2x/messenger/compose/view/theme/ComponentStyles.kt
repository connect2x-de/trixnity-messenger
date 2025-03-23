package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle

data class ComponentStyles(
    // buttons
    val primaryButton: ButtonStyle,
    val secondaryButton: ButtonStyle,
    val commonButton: ButtonStyle,
    val destructiveButton: ButtonStyle,
    // surfaces
    val sidebar: SurfaceStyle,
    val header: SurfaceStyle,
    val timeline: SurfaceStyle,
    // input area
    val inputAreaSurface: SurfaceStyle,
    // messages
    val messageBubbleOwn: SurfaceStyle,
    val messageBubbleOther: SurfaceStyle,
    val messageBubbleError: SurfaceStyle,
)

internal val LocalComponentStyles = staticCompositionLocalOf<ComponentStyles> { error("compositionLocal not defined") }

val MaterialTheme.components: ComponentStyles
    @Composable
    get() = LocalComponentStyles.current
