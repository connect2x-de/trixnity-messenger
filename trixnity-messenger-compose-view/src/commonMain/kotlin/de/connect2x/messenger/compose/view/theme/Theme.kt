package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.getOrNull
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.settings.AppearanceSettingsViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

interface Theme {
    @Composable
    fun create(
        colorScheme: ColorScheme,
        messengerColors: MessengerColors,
        messengerDpConstants: MessengerDpConstants,
        messengerIcons: MessengerIcons,
        shapes: Shapes,
        typography: Typography,
        density: Density,
        content: @Composable () -> Unit,
    )
}

@Composable
fun MessengerTheme(
    colorScheme: ColorScheme = DefaultMessengerColorScheme,
    messengerColors: MessengerColors = DefaultMessengerColors,
    messengerDpConstants: MessengerDpConstants = DefaultMessengerDpConstants,
    messengerIcons: MessengerIcons = DefaultMessengerIcons,
    shapes: Shapes = MaterialTheme.shapes,
    typography: Typography = DefaultMessengerTypography,
    density: Density = DefaultDensity,
    content: @Composable () -> Unit,
) {
    DI.get<Theme>()
        .create(
            colorScheme,
            messengerColors,
            messengerDpConstants,
            messengerIcons,
            shapes,
            typography,
            density,
            content
        )
}

class ThemeImpl : Theme {
    @Composable
    override fun create(
        colorScheme: ColorScheme,
        messengerColors: MessengerColors,
        messengerDpConstants: MessengerDpConstants,
        messengerIcons: MessengerIcons,
        shapes: Shapes,
        typography: Typography,
        density: Density,
        content: @Composable () -> Unit,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = typography
        ) {
            CompositionLocalProvider(
                MessengerColorsProvider provides messengerColors,
                MessengerDpConstantsProvider provides messengerDpConstants,
                MessengerIconsProvider provides messengerIcons,
                LocalDensity provides density
            ) {
                content()
            }
        }
    }
}

private val logger = KotlinLogging.logger {  }

val DefaultDensity: Density
    @Composable
    get() {
        val density = LocalDensity.current
        val settings = DI.getOrNull<MatrixMessengerSettingsHolder>()?.collectAsState()?.value
        val displaySizeFactor = settings?.base?.displaySize ?: DI.get<DefaultSizes>().displaySize
        logger.debug { "Density update: ${Density(density.density * displaySizeFactor, density.fontScale)}, factor: $displaySizeFactor, displaySize: ${settings?.base?.displaySize}" }
        return Density(density.density * displaySizeFactor, density.fontScale)
    }
