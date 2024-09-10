package de.connect2x.messenger.compose.view.common

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

operator fun Color.times(factor: Float): Color {
    return Color(
        (red * factor).coerceIn(0F..1F),
        (green * factor).coerceIn(0F..1F),
        (blue * factor).coerceIn(0F..1F),
        alpha
    )
}

operator fun Color.div(factor: Float): Color {
    return Color(
        (red / factor).coerceIn(0F..1F),
        (green / factor).coerceIn(0F..1F),
        (blue / factor).coerceIn(0F..1F),
        alpha
    )
}

inline val Color.minChannel: Float
    get() = minOf(red, green, blue)

inline val Color.maxChannel: Float
    get() = maxOf(red, green, blue)

inline val Color.hue: Float
    get() {
        val max = maxChannel
        val delta = max - minChannel
        return ((if (delta == 0F) 0F
        else when (max) {
            red -> (green - blue) / delta + (if (green < blue) 6F else 0F)
            green -> (blue - red) / delta + 2F
            blue -> (red - green) / delta + 4F
            else -> 0F
        } / 6F) * 360F).coerceIn(0F..360F)
    }

inline val Color.lightness: Float
    get() = ((maxChannel + minChannel) / 2F).coerceIn(0F..1F)

inline val Color.saturation: Float
    get() {
        val max = maxChannel
        val delta = max - minChannel
        return (if (delta == 0F) 0F
        else delta / (1F - (2F * lightness - 1F).absoluteValue)).coerceIn(0F..1F)
    }

// Changes hue of color without changing saturation or lightness (or alpha)
fun Color.deriveFromHue(hue: Float): Color = Color.hsl(hue, saturation, lightness, alpha)
