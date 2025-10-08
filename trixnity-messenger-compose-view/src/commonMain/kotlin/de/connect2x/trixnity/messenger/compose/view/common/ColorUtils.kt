package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import kotlin.math.absoluteValue
import kotlin.math.round


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

/**
 * Calculates the hsv based value of the color. Not to be confused with color luminance!
 */
inline val Color.lightness: Float
    get() = ((maxChannel + minChannel) / 2F).coerceIn(0F..1F)

inline val Color.saturation: Float
    get() {
        val max = maxChannel
        val delta = max - minChannel
        return (if (delta == 0F) 0F
        else delta / (1F - (2F * lightness - 1F).absoluteValue)).coerceIn(0F..1F)
    }

/**
 * Helper method to pick a bright or a dark color that would be readable
 * on the current color based on its luminance.
 */
fun Color.contrastByLuminance(brightColor: Color, darkColor: Color): Color {
    require(brightColor.luminance() > darkColor.luminance()) {
        "dark is brighter than light color: $brightColor <= $darkColor"
    }
    return if (luminance() > 0.66f) darkColor else brightColor
}

/**
 * Sets the hue of the color while preserving luminance, saturation and alpha.
 */
fun Color.deriveFromHue(
    hue: Float,
    saturation: Float = this.saturation,
    lightness: Float = this.lightness,
    alpha: Float = this.alpha
): Color {
    val (luminance, _, _) = convert(ColorSpaces.CieLab)
    val (_, a, b) = Color.hsl(hue, saturation, lightness).convert(ColorSpaces.CieLab)
    return Color(luminance, a, b, alpha, ColorSpaces.CieLab).convert(ColorSpaces.Srgb)
}

@OptIn(ExperimentalStdlibApi::class)
fun Color.toHex(): String =
    HexFormat {
        bytes {
            bytesPerLine = 1
        }
        number {
            minLength = 2
            removeLeadingZeros = false
        }
    }.let { format ->
        "#" +
                round(this.red * 255).toInt().toHexString(format).takeLast(2) +
                round(this.green * 255).toInt().toHexString(format).takeLast(2) +
                round(this.blue * 255).toInt().toHexString(format).takeLast(2) +
                round(this.alpha * 255).toInt().toHexString(format).takeLast(2)
    }
