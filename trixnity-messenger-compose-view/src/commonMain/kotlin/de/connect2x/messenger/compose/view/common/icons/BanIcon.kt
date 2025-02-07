package de.connect2x.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.i18n.I18nView

enum class BanIconType {
    Unbannable, NotUnbannable, Default
}

@Composable
fun BanIcon(type: BanIconType = BanIconType.Default, size: Dp = 24.dp) {
    val i18n = DI.current.get<I18nView>()
    Tooltip(tooltip = {
        when (type) {
            BanIconType.NotUnbannable -> i18n.notUnbannable()
            BanIconType.Unbannable -> i18n.unbannable()
            BanIconType.Default -> i18n.ban()
        }
    }) {
        Gavel(
            when (type) {
                BanIconType.NotUnbannable -> Color.Red
                BanIconType.Unbannable -> Color.Green
                BanIconType.Default -> MaterialTheme.colorScheme.onSurface
            },
            size
        )
    }
}

@Composable
private fun Gavel(color: Color, size: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.Gavel,
            "",
            modifier = Modifier.size(size + 2.dp),
            tint = Color.Black,
        )
        Icon(
            Icons.Default.Gavel,
            "",
            modifier = Modifier.size(size - 4.dp),
            tint = color,
        )
    }
}
