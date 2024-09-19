package de.connect2x.messenger.compose.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal val DefaultMessengerColorScheme: ColorScheme
    @Composable
    get() {
        val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
        log.debug { "theme: ${settings?.base?.themeMode}, high contrast mode: ${settings?.base?.isHighContrast}" }
        return when (settings?.base?.themeMode) {
            ThemeMode.LIGHT -> if (settings.base.isHighContrast) {
                DI.get<ThemeHighContrastLightColorScheme>().create()
            } else {
                DI.get<ThemeLightColorScheme>().create()
            }

            ThemeMode.DARK -> if (settings.base.isHighContrast) {
                DI.get<ThemeHighContrastDarkColorScheme>().create()
            } else {
                DI.get<ThemeDarkColorScheme>().create()
            }

            null -> DI.get<ThemeLightColorScheme>().create()

            else -> if (isSystemInDarkTheme()) {
                if (settings.base.isHighContrast) {
                    DI.get<ThemeHighContrastDarkColorScheme>().create()
                } else {
                    DI.get<ThemeDarkColorScheme>().create()
                }
            } else {
                if (settings.base.isHighContrast) {
                    DI.get<ThemeHighContrastLightColorScheme>().create()
                } else {
                    DI.get<ThemeLightColorScheme>().create()
                }
            }
        }
    }
