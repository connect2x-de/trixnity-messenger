package de.connect2x.messenger.compose.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.hue
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode

internal val DefaultMessengerColors: MessengerColors
    @Composable
    get() {
        val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
        val accentColor = settings?.base?.accentColor?.let { Color(it.toULong()) } ?: DI.get<DefaultAccentColor>().value
        val accentHue = accentColor.hue
        return when (settings?.base?.themeMode) {
            ThemeMode.LIGHT -> DefaultMessengerLightColors(accentHue)
            ThemeMode.DARK -> DefaultMessengerDarkColors(accentHue)
            null -> DefaultMessengerLightColors(accentHue)
            else -> if (isSystemInDarkTheme()) DefaultMessengerDarkColors(accentHue) else DefaultMessengerLightColors(
                accentHue
            )
        }
    }
