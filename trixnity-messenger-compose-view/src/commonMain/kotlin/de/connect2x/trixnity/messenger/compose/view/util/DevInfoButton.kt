package de.connect2x.trixnity.messenger.compose.view.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.buttonPointerModifier
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView

interface DevInfoButton {
    @Composable fun create(onClick: () -> Unit)
}

@Composable
fun DevInfoButton(onClick: () -> Unit) {
    DI.get<DevInfoButton>().create(onClick)
}

class DevInfoButtonImpl : DevInfoButton {
    @Composable
    override fun create(onClick: () -> Unit) {
        val i18n = DI.get<I18nView>()
        Tooltip(i18n.devInfoButtonTooltip()) {
            IconButton(onClick = onClick, modifier = Modifier.buttonPointerModifier()) {
                Icon(Icons.Default.Info, i18n.devInfoButtonTooltip())
            }
        }
    }
}
