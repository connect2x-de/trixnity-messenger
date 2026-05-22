package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

fun Typography.withFontFamily(fontFamily: FontFamily): Typography {
    return this.copy(
        displayLarge = this.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = this.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = this.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = this.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = this.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = this.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = this.titleLarge.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        titleMedium = this.titleMedium.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        titleSmall = this.titleSmall.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        bodyLarge = this.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = this.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = this.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = this.labelLarge.copy(fontFamily = fontFamily, fontWeight = FontWeight.Bold),
        labelMedium = this.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = this.labelSmall.copy(fontFamily = fontFamily, fontSize = 8.sp),
    )
}

fun fontTypeName(weight: FontWeight, style: FontStyle) = buildString {
    if (weight == FontWeight.Normal && style == FontStyle.Normal) append("Regular")
    else {
        append(
            when (weight) {
                FontWeight.Thin -> "Thin"
                FontWeight.ExtraLight -> "ExtraLight"
                FontWeight.Light -> "Light"
                FontWeight.Normal -> ""
                FontWeight.Medium -> "Medium"
                FontWeight.SemiBold -> "SemiBold"
                FontWeight.Bold -> "Bold"
                FontWeight.ExtraBold -> "ExtraBold"
                FontWeight.Black -> "Black"
                else -> throw IllegalArgumentException("cannot map font type")
            }
        )
        append(
            when (style) {
                FontStyle.Normal -> ""
                FontStyle.Italic -> "Italic"
                else -> throw IllegalArgumentException("cannot map font weight")
            }
        )
    }
}

val TextStyle.dp: Dp
    @Composable @ReadOnlyComposable get() = LocalDensity.current.run { lineHeight.toDp() }
