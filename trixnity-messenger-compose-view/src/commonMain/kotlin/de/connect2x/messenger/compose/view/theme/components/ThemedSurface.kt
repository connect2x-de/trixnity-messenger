package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SurfaceStyle(
    val shape: Shape,
    val color: Color,
    val contentColor: Color,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
    val border: BorderStroke?,
) {
    companion object {
        @Composable
        fun default(
            shape: Shape = RectangleShape,
            color: Color = MaterialTheme.colorScheme.surface,
            contentColor: Color = contentColorFor(color),
            tonalElevation: Dp = 0.dp,
            shadowElevation: Dp = 0.dp,
            border: BorderStroke? = null,
        ) = SurfaceStyle(
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
        )
    }
}

@Composable
fun ThemedSurface(
    modifier: Modifier = Modifier,
    style: SurfaceStyle,
    content: @Composable () -> Unit
) = Surface(
    modifier = modifier,
    shape = style.shape,
    color = style.color,
    contentColor = style.contentColor,
    tonalElevation = style.tonalElevation,
    shadowElevation = style.shadowElevation,
    border = style.border,
    content = content,
)
