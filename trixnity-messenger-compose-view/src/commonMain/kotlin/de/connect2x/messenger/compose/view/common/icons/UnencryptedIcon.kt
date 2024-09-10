package de.connect2x.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.TooltipText

@Composable
fun UnencryptedIcon() {
    val i18n = DI.current.get<I18nView>()
    Tooltip({ TooltipText(i18n.roomTypeUnencrypted()) }) {
        Icon(
            Icons.Default.NoEncryption,
            i18n.roomTypeUnencrypted(),
            Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondary,
        )
    }
}