package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.TextStyle
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal val DefaultMessengerTypography: Typography
    @Composable
    get() {
        fun TextStyle.applySizeFactor(factor: Float): TextStyle =
            this.copy(fontSize = this.fontSize * factor)

        val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
        log.debug { "Font size: ${settings?.base?.fontSize}" }
        val fontFactor = when (settings?.base?.fontSize) {
            0 -> 0.7f
            1 -> 0.8f
            2 -> 0.9f
            3 -> 1.0f
            4 -> 1.1f
            5 -> 1.2f
            6 -> 1.3f
            else -> 1.0f
        }

        val typography = DI.get<ThemeTypography>().create()
        return when (fontFactor) {
            null -> typography
            else -> typography.copy(
                displayLarge = typography.displayLarge.applySizeFactor(fontFactor),
                displayMedium = typography.displayMedium.applySizeFactor(fontFactor),
                displaySmall = typography.displaySmall.applySizeFactor(fontFactor),
                headlineLarge = typography.headlineLarge.applySizeFactor(fontFactor),
                headlineMedium = typography.headlineMedium.applySizeFactor(fontFactor),
                headlineSmall = typography.headlineSmall.applySizeFactor(fontFactor),
                titleLarge = typography.titleLarge.applySizeFactor(fontFactor),
                titleMedium = typography.titleMedium.applySizeFactor(fontFactor),
                titleSmall = typography.titleSmall.applySizeFactor(fontFactor),
                bodyLarge = typography.bodyLarge.applySizeFactor(fontFactor),
                bodyMedium = typography.bodyMedium.applySizeFactor(fontFactor),
                bodySmall = typography.bodySmall.applySizeFactor(fontFactor),
                labelLarge = typography.labelLarge.applySizeFactor(fontFactor),
                labelMedium = typography.labelMedium.applySizeFactor(fontFactor),
                labelSmall = typography.labelSmall.applySizeFactor(fontFactor)
            )
        }
    }
