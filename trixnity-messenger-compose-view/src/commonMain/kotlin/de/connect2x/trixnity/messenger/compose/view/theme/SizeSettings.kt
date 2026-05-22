package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Density
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.getOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Immutable
data class SizeSettings(val displaySize: Float?, val fontSize: Float?) {
    @Stable
    fun toDensity(system: Density, fallback: DefaultSizes) =
        Density(
            density = system.density * (displaySize ?: fallback.displaySize),
            fontScale = system.fontScale * (fontSize ?: fallback.fontSize),
        )
}

internal val CurrentSizeSettings: SizeSettings
    @Composable
    get() =
        DI.getOrNull<MatrixMessengerSettingsHolder>()
            ?.map { SizeSettings(it.base.displaySize, it.base.fontSize) }
            ?.distinctUntilChanged()
            ?.collectAsState(null)
            ?.value ?: SizeSettings(null, null)
