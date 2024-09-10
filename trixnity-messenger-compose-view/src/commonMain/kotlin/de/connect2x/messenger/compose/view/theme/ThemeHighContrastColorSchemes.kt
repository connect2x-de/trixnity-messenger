package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.times
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

interface ThemeHighContrastLightColorScheme {
    @Composable
    fun create(): ColorScheme
}

class ThemeHighContrastLightColorSchemeImpl : ThemeHighContrastLightColorScheme {
    @Composable
    override fun create(): ColorScheme = makeHighContrastColorScheme(DI.current.get<ThemeLightColorScheme>().create())
        .also { log.debug { "create default high contrast color scheme from light" } }
}

interface ThemeHighContrastDarkColorScheme {
    @Composable
    fun create(): ColorScheme
}

class ThemeHighContrastDarkColorSchemeImpl : ThemeHighContrastDarkColorScheme {
    @Composable
    override fun create(): ColorScheme =
        makeHighContrastColorScheme(DI.current.get<ThemeDarkColorScheme>().create(), true)
            .also { log.debug { "create default high contrast color scheme from dark" } }
}

private fun makeHighContrastColorScheme(scheme: ColorScheme, isDarkTheme: Boolean = false): ColorScheme {
    val foreground = if (isDarkTheme) Color.Black else Color.White
    val background = if (isDarkTheme) Color.White else Color.Black
    return ColorScheme(
        primary = background,
        onPrimary = foreground,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        secondary = background * 0.8F,
        onSecondary = foreground,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,
        tertiary = background * 0.7F,
        onTertiary = foreground,
        tertiaryContainer = scheme.tertiaryContainer,
        onTertiaryContainer = scheme.onTertiaryContainer,
        error = scheme.error,
        errorContainer = scheme.errorContainer,
        onError = scheme.onError,
        onErrorContainer = scheme.onErrorContainer,
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        outline = scheme.outline,
        inverseOnSurface = scheme.inverseOnSurface,
        inverseSurface = scheme.inverseSurface,
        inversePrimary = scheme.inversePrimary,
        surfaceTint = scheme.surfaceTint,
        outlineVariant = scheme.outlineVariant,
        scrim = scheme.scrim,
    )
}
