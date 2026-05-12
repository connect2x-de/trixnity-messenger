package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
data class ThemeSettings(
    val themeMode: ThemeMode,
    val isHighContrast: Boolean,
    val accentColor: Color?,
) {
    @Composable
    fun isDarkMode(): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> isDarkTheme()
    }
}

@ExperimentalThemingApi
val CurrentThemeSettings: ThemeSettings
    @Composable
    get() = DI.getOrNull<MatrixMessengerSettingsHolder>()
        ?.map {
            ThemeSettings(
                it.base.themeMode,
                it.base.isHighContrast,
                it.base.accentColor?.let { Color(it.toULong()) })
        }
        ?.distinctUntilChanged()
        ?.collectAsState(null)?.value
        ?: ThemeSettings(ThemeMode.DEFAULT, false, null)

@Composable
expect fun isDarkTheme(): Boolean
