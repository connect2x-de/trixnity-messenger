package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle

interface ThemeComponents {
    @Composable
    fun create(): ComponentStyles
}

class ThemeComponentsImpl : ThemeComponents {
    @Composable
    override fun create(): ComponentStyles = ComponentStyles(
        // buttons
        primaryButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ),
        secondaryButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ),
        commonButton = ButtonStyle.filled(
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.secondary,
            ),
        ),
        destructiveButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        ),
    )
}
