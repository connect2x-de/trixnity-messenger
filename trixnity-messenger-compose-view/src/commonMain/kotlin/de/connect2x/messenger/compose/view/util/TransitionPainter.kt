package de.connect2x.messenger.compose.view.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter


/**
 * Normally, to transition between two images, we'd fade alpha linearly. Image1 would go from 1.0 to 0.0,
 * Image2 would go from 0.0 to 1.0, with the total opacity at any point adding up to 1.0.
 *
 * Sadly, compose interprets alpha values as sRGB, which is roughly equivalent to a 2.2 gamma.
 *
 * That means at the middle point, the opacity of each image would be 0.5 ^ 2.2 ~ 0.22,
 * with a total opacity of 0.44 instead of 1.0, causing a distracting dip in brightness.
 *
 * To compensate for that, we calculate the entire transition in linear colorspace,
 * and only convert to sRGB at the very end.
 */
internal class TransitionPainter(
    private val image: Painter?,
    private val fallback: Painter?,
    private val opacity: State<Float>
): Painter() {
    override val intrinsicSize: Size = image?.intrinsicSize ?: fallback?.intrinsicSize ?: Size.Zero

    override fun DrawScope.onDraw() {
        val imageAlpha = toSrgb(opacity.value)
        val fallbackAlpha = toSrgb(1f - opacity.value)

        if (fallbackAlpha != 0f) fallback?.drawTo(this, fallbackAlpha)
        if (imageAlpha != 0f) image?.drawTo(this, imageAlpha)
    }
}

private fun toSrgb(value: Float): Float {
    return ColorSpaces.Srgb.oetf(value.toDouble()).toFloat()
}

private fun Painter.drawTo(scope: DrawScope, alpha: Float) {
    scope.draw(scope.size, alpha = alpha)
}

@Composable
internal fun animateImage(image: Painter?, fallback: Painter?): Painter? {
    val opacity = animateFloatAsState(if (image == null) 0f else 1f)
    return if (fallback == null) image else TransitionPainter(image, fallback, opacity)
}
