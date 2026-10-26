package de.connect2x.trixnity.messenger.compose.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val LocalAccountColorsEnabled = staticCompositionLocalOf { true }

@Composable
internal fun AccountColorBar(accountColor: Long?, modifier: Modifier = Modifier) {
    if (!LocalAccountColorsEnabled.current) return
    Box(
        modifier
            .width(if (accountColor != null) 6.dp else 0.dp)
            .background(if (accountColor != null) Color(accountColor) else Color.Transparent)
    )
}
