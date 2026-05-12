package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.hsl
import androidx.compose.ui.graphics.luminance
import de.connect2x.trixnity.core.model.UserId
import kotlin.math.absoluteValue

@Immutable
data class MessengerColors(
    val success: Color,
    val presenceOnline: Color,
    val presenceOffline: Color,
    val presenceUnavailable: Color,
    val verificationTrusted: Color,
    val verificationUntrusted: Color,
    val verificationNeutral: Color,
    val metaDataPreview: Color,
    val metaDataPreviewBackground: Color,
    val blockedUser: Color,
    val warning: Color,
    val link: Color,
    val linkByMe: Color,
    val userColors: List<Color>,
) {
    @Stable
    @Deprecated("use getUserColor(userId, backgroundColor) that guarantees a good contrast to the background color")
    fun getUserColor(userId: UserId): Color = userColors[(userId.hashCode() % userColors.size).absoluteValue]

    @Composable
    fun getUserColor(userId: UserId, backgroundColor: Color): Color =
         increaseContrast(@Suppress("DEPRECATION") getUserColor(userId), backgroundColor)
}

val MessengerColorsProvider = staticCompositionLocalOf<MessengerColors> { error("compositionLocal not defined") }

val MaterialTheme.messengerColors: MessengerColors
    @Composable
    @ReadOnlyComposable
    get() = MessengerColorsProvider.current

/** modifies the `color` until a contrast ratio of `minRatio` to the `backgroundColor` is achieved */
private fun increaseContrast(color: Color, backgroundColor: Color, minRatio: Double = 4.5): Color {
    if (contrastRatio(color, backgroundColor) >= minRatio) return color

    val (hue, saturation, _) = color.toHsl()

    // Try different lightness values to find one with good contrast
    val lightnessRange =
        if (backgroundColor.luminance() > 0.5) (step(0.1f..0.35f))
        else step(0.65f..0.9f)

    for (lightness in lightnessRange) {
        val adjusted = hsl(hue, saturation, lightness)
        if (contrastRatio(adjusted, backgroundColor) >= minRatio) {
            return adjusted
        }
    }

    return color
}

private fun contrastRatio(color1: Color, color2: Color): Double {
    val lum1 = color1.luminance()
    val lum2 = color2.luminance()
    val lighter = maxOf(lum1, lum2)
    val darker = minOf(lum1, lum2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val lightness = (max + min) / 2f

    val saturation = if (delta == 0f) 0f else {
        delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    }.coerceIn(0f, 1f)

    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0) it + 360f else it }

    return Triple(hue, saturation, lightness)
}

private fun step(range: ClosedRange<Float>, step: Float = 0.05f): Sequence<Float> = sequence {
    var value = range.start
    while (value <= range.endInclusive) {
        yield(value)
        value += step
    }
}
