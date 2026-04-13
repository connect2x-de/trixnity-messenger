package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.trixnity.messenger.FontKind
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.multi.MatrixMultiMessengerConfiguration
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal val CurrentFontKind: FontKind
    @Composable
    get() {
        val config = DI.get<MatrixMultiMessengerConfiguration>()
        val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()
        return if (!config.enableBundledFont) FontKind.SYSTEM
        else settings
            ?.map { it.base.fontKind }
            ?.distinctUntilChanged()
            ?.collectAsState(null)?.value
            ?: FontKind.BUNDLED
    }
