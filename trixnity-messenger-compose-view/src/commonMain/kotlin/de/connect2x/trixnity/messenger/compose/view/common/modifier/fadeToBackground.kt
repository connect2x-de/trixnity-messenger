package de.connect2x.trixnity.messenger.compose.view.common.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
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
 * @param destinationAlpha the alpha at the bottom of the gradient (between 0 (elements are fully faded into the
 *   background) and 1 (there is no fade))
 */
fun Modifier.fadeBottomToBackground(gradientOffset: Dp, destinationAlpha: Float): Modifier {
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
        val gradient = Brush.verticalGradient(0.0f to Color.Black, 1.0f to Color.Black.copy(alpha = destinationAlpha))
        onDrawWithContent {
            drawContent()
            drawRect(gradient, topLeft = Offset(x = 0f, gradientOffset.toPx()), blendMode = BlendMode.DstIn)
        }
    }
}
