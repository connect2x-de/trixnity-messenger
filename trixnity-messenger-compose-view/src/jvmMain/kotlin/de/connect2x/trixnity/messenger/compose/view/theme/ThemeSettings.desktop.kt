package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import org.jetbrains.skiko.SystemTheme
import org.jetbrains.skiko.currentSystemTheme

// since Compose itself does not update the value on its own, use this neat trick
// see https://youtrack.jetbrains.com/issue/CMP-1986/isSystemInDarkTheme-should-dynamically-update-when-system-theme-is-changed#focus=Comments-27-12665870.0-0
@Composable
actual fun isDarkTheme(): Boolean {
    return produceState(initialValue = currentSystemTheme == SystemTheme.DARK) {
        while (true) {
            delay(5_000)
            value = currentSystemTheme == SystemTheme.DARK
        }
    }.value
}


