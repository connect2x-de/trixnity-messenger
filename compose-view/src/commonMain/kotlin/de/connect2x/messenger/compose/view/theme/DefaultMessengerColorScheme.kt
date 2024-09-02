package de.connect2x.messenger.compose.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal val DefaultMessengerColorScheme: ColorScheme
    @Composable
    get() {
        val settings = DI.current.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
        log.debug { "theme: ${settings?.base?.themeMode}, high contrast mode: ${settings?.base?.isHighContrast}" }
        return when (settings?.base?.themeMode) {
            ThemeMode.LIGHT -> if (settings.base.isHighContrast) {
                DI.current.get<ThemeHighContrastLightColorScheme>().create()
            } else {
                DI.current.get<ThemeLightColorScheme>().create()
            }

            ThemeMode.DARK -> if (settings.base.isHighContrast) {
                DI.current.get<ThemeHighContrastDarkColorScheme>().create()
            } else {
                DI.current.get<ThemeDarkColorScheme>().create()
            }

            null -> DI.current.get<ThemeLightColorScheme>().create()

            else -> if (isSystemInDarkTheme()) {
                if (settings.base.isHighContrast) {
                    DI.current.get<ThemeHighContrastDarkColorScheme>().create()
                } else {
                    DI.current.get<ThemeDarkColorScheme>().create()
                }
            } else {
                if (settings.base.isHighContrast) {
                    DI.current.get<ThemeHighContrastLightColorScheme>().create()
                } else {
                    DI.current.get<ThemeLightColorScheme>().create()
                }
            }
        }
    }
