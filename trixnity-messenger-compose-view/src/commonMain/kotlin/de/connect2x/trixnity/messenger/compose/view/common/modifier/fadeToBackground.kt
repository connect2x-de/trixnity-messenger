package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Fade bottom edge to background colour
 *
 * @param gradientOffset the offset of the gradient from the top
 * @param colorStops the colors to use as part of the fading gradient (e.g 0f to [Color.Black] and 1f to
 *   [Color.Transparent] for a gradient that starts at the given offset and becomes fully transparent at the end)
 */
fun Modifier.fadeBottomToBackground(gradientOffset: Dp, colorStops: List<Pair<Float, Color>>): Modifier {
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
        val gradientOffsetPx = gradientOffset.toPx()
        val gradient =
            if (size.height > gradientOffsetPx) {
                Brush.verticalGradient(colorStops = colorStops.toTypedArray(), startY = gradientOffsetPx)
            } else null
        onDrawWithContent {
            drawContent()
            gradient?.let { drawRect(it, blendMode = BlendMode.DstIn) }
        }
    }
}
