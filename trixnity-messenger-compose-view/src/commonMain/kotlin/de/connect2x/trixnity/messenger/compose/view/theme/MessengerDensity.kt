package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get

val SystemDensity = compositionLocalOf<Density> { error("compositionLocal not defined") }

val DefaultMessengerDensity: Density
    @Composable
    get() = CurrentSizeSettings.toDensity(LocalDensity.current, DI.get<DefaultSizes>())
