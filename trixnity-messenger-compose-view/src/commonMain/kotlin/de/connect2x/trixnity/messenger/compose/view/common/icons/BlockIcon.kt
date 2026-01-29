package de.connect2x.trixnity.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView

@Composable
fun BlockIcon(size: Dp = 24.dp) {
    val i18n = DI.get<I18nView>()
    Tooltip({ Text(i18n.block()) }) {
        Icon(Icons.Outlined.Lock, i18n.block(), Modifier.size(size))
    }
}
