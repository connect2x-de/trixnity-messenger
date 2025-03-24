package de.connect2x.messenger.compose.view.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.messenger.compose.view.theme.components.TooltipStyle

interface ThemeComponents {
    @Composable
    fun create(): ComponentStyles
}

class ThemeComponentsImpl : ThemeComponents {
    @OptIn(ExperimentalMaterial3Api::class)
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
        // tooltip
        tooltip = TooltipStyle.default(
            contentPadding = PaddingValues(5.dp),
            colors = TooltipDefaults.richTooltipColors(
                contentColor = MaterialTheme.colorScheme.onTertiary,
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
            textStyle = MaterialTheme.typography.bodySmall,
        )
    )
}
