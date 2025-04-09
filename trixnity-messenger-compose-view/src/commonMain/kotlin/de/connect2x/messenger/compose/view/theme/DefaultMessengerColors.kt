package de.connect2x.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get

internal val DefaultMessengerColors: MessengerColors
    @Composable
    get() {
        val settings = CurrentThemeSettings
        val accentColor = settings.accentColor ?: DI.get<DefaultAccentColor>().value

        return if (settings.isDarkMode()) {
            DefaultMessengerDarkColors.create(accentColor)
        } else {
            DefaultMessengerLightColors.create(accentColor)
        }
    }
