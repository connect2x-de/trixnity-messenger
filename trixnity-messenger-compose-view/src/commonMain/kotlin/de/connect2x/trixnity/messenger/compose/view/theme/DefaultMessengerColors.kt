package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get

@OptIn(ExperimentalThemingApi::class)
internal val DefaultMessengerColors: MessengerColors
    @Composable
    get() {
        val settings = CurrentThemeSettings
        val accentColor = settings.accentColor ?: DI.get<DefaultAccentColor>().value

        return if (settings.isDarkMode()) {
            DI.get<ThemeDarkMessengerColors>().create(accentColor)
        } else {
            DI.get<ThemeLightMessengerColors>().create(accentColor)
        }
    }
