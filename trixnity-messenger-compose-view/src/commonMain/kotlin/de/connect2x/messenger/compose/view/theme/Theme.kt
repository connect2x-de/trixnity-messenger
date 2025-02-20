package de.connect2x.messenger.compose.view.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get

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
    typography: Typography = DI.get<ThemeTypography>().create(),
    density: Density = DefaultMessengerDensity,
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
                LocalDensity provides density,
                SystemDensity provides LocalDensity.current
            ) {
                content()
            }
        }
    }
}
