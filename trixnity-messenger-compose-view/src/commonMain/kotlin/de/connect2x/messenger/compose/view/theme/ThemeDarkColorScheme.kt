package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.common.deriveFromHue
import de.connect2x.messenger.compose.view.common.hue
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger { }

interface ThemeDarkColorScheme {
    fun create(accentColor: Color): ColorScheme
}

class ThemeDarkColorSchemeImpl : ThemeDarkColorScheme {
    override fun create(accentColor: Color): ColorScheme {
        val accentHue = accentColor.hue
        return darkColorScheme(
            primary = accentColor,
            onPrimary = md_theme_dark_onPrimary,
            primaryContainer = md_theme_dark_primaryContainer.deriveFromHue(accentHue),
            onPrimaryContainer = md_theme_dark_onPrimaryContainer,
            secondary = md_theme_dark_secondary.deriveFromHue(accentHue),
            onSecondary = md_theme_dark_onSecondary,
            secondaryContainer = md_theme_dark_secondaryContainer,
            onSecondaryContainer = md_theme_dark_onSecondaryContainer,
            tertiary = md_theme_dark_tertiary,
            onTertiary = md_theme_dark_onTertiary,
            tertiaryContainer = md_theme_dark_tertiaryContainer,
            onTertiaryContainer = md_theme_dark_onTertiaryContainer,
            error = md_theme_dark_error,
            errorContainer = md_theme_dark_errorContainer,
            onError = md_theme_dark_onError,
            onErrorContainer = md_theme_dark_onErrorContainer,
            background = md_theme_dark_background,
            onBackground = md_theme_dark_onBackground,
            surface = md_theme_dark_surface,
            onSurface = md_theme_dark_onSurface,
            surfaceVariant = md_theme_dark_surfaceVariant.deriveFromHue(accentHue),
            onSurfaceVariant = md_theme_dark_onSurfaceVariant,
            outline = md_theme_dark_outline.deriveFromHue(accentHue),
            inverseOnSurface = md_theme_dark_inverseOnSurface,
            inverseSurface = md_theme_dark_inverseSurface.deriveFromHue(accentHue),
            inversePrimary = md_theme_dark_inversePrimary.deriveFromHue(accentHue),
            surfaceTint = md_theme_dark_surfaceTint.deriveFromHue(accentHue),
            outlineVariant = md_theme_dark_outlineVariant,
            scrim = md_theme_dark_scrim,
            surfaceDim = md_theme_dark_surfaceDim.deriveFromHue(accentHue),
            surfaceBright = md_theme_dark_surfaceBright.deriveFromHue(accentHue),
            surfaceContainerLowest = md_theme_dark_surfaceContainerLowest.deriveFromHue(accentHue),
            surfaceContainerLow = md_theme_dark_surfaceContainerLow.deriveFromHue(accentHue),
            surfaceContainer = md_theme_dark_surfaceContainer.deriveFromHue(accentHue),
            surfaceContainerHigh = md_theme_dark_surfaceContainerHigh.deriveFromHue(accentHue),
            surfaceContainerHighest = md_theme_dark_surfaceContainerHighest.deriveFromHue(accentHue),
        )
            .also { log.debug { "create default dark color scheme" } }
    }
}
