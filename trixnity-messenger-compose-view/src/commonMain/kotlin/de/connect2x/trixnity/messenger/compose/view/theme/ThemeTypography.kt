package de.connect2x.trixnity.messenger.compose.view.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

interface ThemeTypography {
    @Composable
    fun create(): Typography

    companion object
}

class ThemeTypographySystem : ThemeTypography {
    @Composable
    override fun create(): Typography {
        return Typography()
    }
}
