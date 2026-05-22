package de.connect2x.trixnity.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView

@Composable
fun BoxScope.PublicIcon() {
    val i18n = DI.get<I18nView>()
    Box(Modifier.align(Alignment.BottomEnd), contentAlignment = Alignment.Center) {
        Tooltip({ Text(i18n.roomTypePublic()) }) {
            Icon(
                Icons.Default.Circle,
                i18n.roomTypeUnencrypted(),
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.background,
            )
        }
        Icon(
            Icons.Default.Public,
            i18n.roomTypePublic(),
            Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSecondary,
        )
    }
}
