package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle

data class ComponentStyles(
    // buttons
    val primaryButton: ButtonStyle,
    val secondaryButton: ButtonStyle,
    val commonButton: ButtonStyle,
    val destructiveButton: ButtonStyle,
)

internal val LocalComponentStyles = staticCompositionLocalOf<ComponentStyles> { error("compositionLocal not defined") }

val MaterialTheme.components: ComponentStyles
    @Composable
    get() = LocalComponentStyles.current
