package de.connect2x.trixnity.messenger.compose.view.theme.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import de.connect2x.trixnity.messenger.compose.view.theme.components

data class SystemUiStyle(
    val header: Color,
    val footer: Color,
    val isDarkMode: Boolean,
) {
    companion object {
        @Composable
        fun default(
            header: Color = MaterialTheme.colorScheme.background,
            footer: Color = header,
            isDarkMode: Boolean = MaterialTheme.colorScheme.background.luminance() <= MaterialTheme.colorScheme.onBackground.luminance(),
        ) = SystemUiStyle(
            header = header,
            footer = footer,
            isDarkMode = isDarkMode,
        )
    }
}

@Composable
expect fun ApplySystemUiTheme(
    style: SystemUiStyle = MaterialTheme.components.systemUi,
)
