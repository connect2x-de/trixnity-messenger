package de.connect2x.messenger.compose.view.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.messenger.compose.view.theme.components.IconButtonStyle
import de.connect2x.messenger.compose.view.theme.components.InputAreaStyle
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle
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
        commonButton = ButtonStyle.outlined(),
        destructiveButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        ),
        primaryIconButton = IconButtonStyle.filled(
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        ),
        secondaryIconButton = IconButtonStyle.filled(
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        ),
        commonIconButton = IconButtonStyle.default(
            colors = IconButtonDefaults.iconToggleButtonColors(
                // TODO: We shouldn't use tertiary but it's black in darkmode with the default. Theme might be broken?
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
        ),
        destructiveIconButton = IconButtonStyle.default(
            colors = IconButtonDefaults.iconToggleButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ),
        // surfaces
        background = SurfaceStyle.default(),
        dialog = SurfaceStyle.default(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        popup = SurfaceStyle.default(
            shadowElevation = 4.dp,
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium
        ),
        sidebar = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
        ),
        header = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ),
        timeline = SurfaceStyle.default(),
        // room list
        roomListSelection = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
        accountSelector = ButtonStyle.filled(
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
            ),
            elevation = null,
        ),
        // input area
        inputAreaSurface = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ),
        inputArea = InputAreaStyle.default(
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                cursorColor = MaterialTheme.colorScheme.onSurface,

                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,

                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),

                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = PaddingValues(7.dp),
        ),
        // file viewer
        fileViewerSurface = SurfaceStyle.default(
            color = Color.Black,
            contentColor = Color.LightGray,
        ),
        fileViewerIconButton = IconButtonStyle.filled(
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.DarkGray,
                contentColor = Color.LightGray,
            ),
        ),
        // message bubbles
        messageBubbleOwn = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(8.dp),
        ),
        messageBubbleOther = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = RoundedCornerShape(8.dp),
        ),
        messageBubbleError = SurfaceStyle.default(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(8.dp),
        ),
        messageReference = SurfaceStyle.default(
            shape = RoundedCornerShape(8.dp),
            color = Color(0x55FFFFFF),
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
