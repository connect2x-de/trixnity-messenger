package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified

@Composable
fun Color.withContentColor(enabled: Boolean = true): Color =
    if (isSpecified) this
    else if (enabled) LocalContentColor.current
    else LocalContentColor.current.copy(alpha = 0.38f)

/**
 * ThemedButtons and ThemedIconButtons map Color.Content to LocalContentColor.current in their local scope
 */
val Color.Companion.LocalContent get() = Color.Unspecified
