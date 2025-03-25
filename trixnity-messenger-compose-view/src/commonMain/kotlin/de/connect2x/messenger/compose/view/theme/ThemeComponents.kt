package de.connect2x.messenger.compose.view.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.theme.components.ButtonStyle
import de.connect2x.messenger.compose.view.theme.components.SurfaceStyle

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
        commonButton = ButtonStyle.outlined(),
        destructiveButton = ButtonStyle.filled(
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
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
        )
    )
}
