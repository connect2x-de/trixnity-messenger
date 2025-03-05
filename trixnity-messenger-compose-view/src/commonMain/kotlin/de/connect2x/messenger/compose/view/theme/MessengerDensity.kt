package de.connect2x.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder

val SystemDensity = compositionLocalOf<Density> { error("compositionLocal not defined") }
val DefaultMessengerDensity: Density
    @Composable
    get() {
        val systemDensity = LocalDensity.current
        val defaultSizes = DI.get<DefaultSizes>()
        val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
        return Density(
            density = systemDensity.density * (settings?.base?.displaySize ?: defaultSizes.displaySize),
            fontScale = systemDensity.fontScale * (settings?.base?.fontSize ?: defaultSizes.fontSize)
        )
    }
