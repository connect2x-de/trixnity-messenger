package de.connect2x.messenger.compose.view.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SurfaceStyle(
    val shape: Shape,
    val color: Color,
    val contentColor: Color,
    val tonalElevation: Dp,
    val shadowElevation: Dp,
    val border: BorderStroke?,
    val contentPadding: PaddingValues,
    val padding: PaddingValues,
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
            contentPadding: PaddingValues = PaddingValues(0.dp),
            padding: PaddingValues = PaddingValues(0.dp),
        ) = SurfaceStyle(
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            contentPadding = contentPadding,
            padding = padding,
        )
    }
}

@Composable
fun ThemedSurface(
    modifier: Modifier = Modifier,
    style: SurfaceStyle,
    content: @Composable () -> Unit
) = Surface(
    modifier = modifier.padding(style.padding),
    shape = style.shape,
    color = style.color,
    contentColor = style.contentColor,
    tonalElevation = style.tonalElevation,
    shadowElevation = style.shadowElevation,
    border = style.border,
) {
    Box(Modifier.padding(style.contentPadding)) {
        content()
    }
}

@Composable
fun Modifier.themedSurface(
    style: SurfaceStyle,
): Modifier {
    val shadowElevation = with (LocalDensity.current) { style.shadowElevation.toPx() }
    val backgroundColor = if (style.color != MaterialTheme.colorScheme.surface) style.color
    else MaterialTheme.colorScheme.surfaceColorAtElevation(style.tonalElevation)
    val shadowModifier = Modifier.graphicsLayer(
        shadowElevation = shadowElevation,
        shape = style.shape,
        clip = false
    )

    return this
        .padding(style.padding)
        .then(if (shadowElevation > 0f) shadowModifier else Modifier)
        .then(if (style.border != null) Modifier.border(style.border, style.shape) else Modifier)
        .background(color = backgroundColor, shape = style.shape)
        .clip(style.shape)
        .padding(style.contentPadding)
}
