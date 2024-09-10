package de.connect2x.messenger.compose.view.common.icons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
fun HelpIcon(text: String) {
    val i18n = DI.current.get<I18nView>()
    Spacer(Modifier.size(10.dp))
    Tooltip(tooltip = {
        TooltipText(text)
    }) {
        Icon(Icons.Outlined.Info, i18n.commonHelp(), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
