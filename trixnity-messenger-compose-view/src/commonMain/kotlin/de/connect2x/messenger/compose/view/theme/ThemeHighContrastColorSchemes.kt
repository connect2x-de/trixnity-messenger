package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.common.times
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

interface ThemeHighContrastLightColorScheme {
    fun create(accentColor: Color): ColorScheme
}

class ThemeHighContrastLightColorSchemeImpl(
    private val wrapped: ThemeLightColorScheme,
) : ThemeHighContrastLightColorScheme {
    override fun create(accentColor: Color): ColorScheme = makeHighContrastColorScheme(wrapped.create(accentColor))
        .also { log.debug { "create default high contrast color scheme from light" } }
}

interface ThemeHighContrastDarkColorScheme {
    fun create(accentColor: Color): ColorScheme
}

class ThemeHighContrastDarkColorSchemeImpl(
    private val wrapped: ThemeDarkColorScheme,
) : ThemeHighContrastDarkColorScheme {
    override fun create(accentColor: Color): ColorScheme =
        makeHighContrastColorScheme(wrapped.create(accentColor), true)
            .also { log.debug { "create default high contrast color scheme from dark" } }
}

private fun makeHighContrastColorScheme(scheme: ColorScheme, isDarkTheme: Boolean = false): ColorScheme {
    val foreground = if (isDarkTheme) Color.Black else Color.White
    val background = if (isDarkTheme) Color.White else Color.Black

    // TODO: we do not currently have proper fixed colors
    return ColorScheme(
        primary = background,
        onPrimary = foreground,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        inversePrimary = scheme.inversePrimary,
        secondary = background * 0.8F,
        onSecondary = foreground,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,
        tertiary = background * 0.7F,
        onTertiary = foreground,
        tertiaryContainer = scheme.tertiaryContainer,
        onTertiaryContainer = scheme.onTertiaryContainer,
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        surfaceTint = scheme.surfaceTint,
        inverseSurface = scheme.inverseSurface,
        inverseOnSurface = scheme.inverseOnSurface,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
        outline = scheme.outline,
        outlineVariant = scheme.outlineVariant,
        scrim = scheme.scrim,
        surfaceBright = scheme.surfaceBright,
        surfaceDim = scheme.surfaceDim,
        surfaceContainer = scheme.surfaceContainer,
        surfaceContainerHigh = scheme.surfaceContainerHigh,
        surfaceContainerHighest = scheme.surfaceContainerHighest,
        surfaceContainerLow = scheme.surfaceContainerLow,
        surfaceContainerLowest = scheme.surfaceContainerLowest,
    )
}
