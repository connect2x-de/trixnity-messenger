package de.connect2x.messenger.compose.view.theme

import androidx.compose.ui.graphics.Color

interface DefaultAccentColor {
    val value: Color
}

class DefaultAccentColorImpl : DefaultAccentColor {
    override val value: Color = Color(0xfffa7c2e)
}
