package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb

@Composable
actual fun ApplySystemUiTheme(style: SystemUiStyle) {
    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(style, activity) {
        if (style.isDarkMode) {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(style.header.toArgb()),
                navigationBarStyle = SystemBarStyle.dark(style.footer.toArgb())
            )
        } else {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(style.header.toArgb(), style.header.toArgb()),
                navigationBarStyle = SystemBarStyle.light(style.footer.toArgb(), style.footer.toArgb())
            )
        }
    }
}
