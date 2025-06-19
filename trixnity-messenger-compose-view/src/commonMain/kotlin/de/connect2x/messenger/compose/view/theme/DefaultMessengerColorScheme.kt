package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal interface MessengerColorScheme {
    @Stable
    fun create(isDarkMode: Boolean, isHighContrast: Boolean, accentColor: Color): ColorScheme
}

internal class MessengerColorSchemeImpl(
    private val themeLight: ThemeLightColorScheme,
    private val themeLightContrast: ThemeHighContrastLightColorScheme,
    private val themeDark: ThemeDarkColorScheme,
    private val themeDarkContrast: ThemeHighContrastDarkColorScheme,
): MessengerColorScheme {
    @Stable
    override fun create(isDarkMode: Boolean, isHighContrast: Boolean, accentColor: Color): ColorScheme =
        if (isDarkMode) {
            if (isHighContrast) themeDarkContrast.create(accentColor)
            else themeDark.create(accentColor)
        } else {
            if (isHighContrast) themeLightContrast.create(accentColor)
            else themeLight.create(accentColor)
        }
}

@OptIn(ExperimentalThemingApi::class)
internal val DefaultMessengerColorScheme: ColorScheme
    @Composable
    get() {
        val settings = CurrentThemeSettings
        log.debug { "theme: $settings" }
        return DI.get<MessengerColorScheme>().create(
            isDarkMode = settings.isDarkMode(),
            isHighContrast = settings.isHighContrast,
            accentColor = settings.accentColor ?: DI.get<DefaultAccentColor>().value
        )
    }
