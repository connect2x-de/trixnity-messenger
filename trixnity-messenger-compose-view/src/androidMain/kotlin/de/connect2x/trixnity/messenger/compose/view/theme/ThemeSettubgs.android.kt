package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable actual fun isDarkTheme(): Boolean = isSystemInDarkTheme() // on Android, this works oot of the box
