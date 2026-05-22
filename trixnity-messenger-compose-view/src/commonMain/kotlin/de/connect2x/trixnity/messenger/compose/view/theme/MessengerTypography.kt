package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.FontKind
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.getOrNull
import org.koin.core.qualifier.named

private val log = Logger("de.connect2x.trixnity.messenger.compose.view.theme.DefaultMessengerTypography")

val DefaultMessengerTypography: Typography
    @Composable
    get() =
        when (CurrentFontKind) {
            FontKind.SYSTEM -> SystemTypography
            FontKind.BUNDLED -> BundledTypography
        }

private val SystemTypography: Typography
    @Composable get() = DI.get<ThemeTypography>(named(FontKind.SYSTEM)).create()

private val BundledTypography: Typography
    @Composable
    get() =
        DI.getOrNull<ThemeTypography>(named(FontKind.BUNDLED))?.create()
            ?: run {
                log.warn { "Using bundled Font without registering ThemeTypography: this is a misconfiguration" }
                SystemTypography
            }
