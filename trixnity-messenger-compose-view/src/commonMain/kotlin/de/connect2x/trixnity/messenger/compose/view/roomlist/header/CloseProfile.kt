package de.connect2x.trixnity.messenger.compose.view.roomlist.header

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton

interface CloseProfileView {
    @Composable fun create(closeProfile: () -> Unit)
}

@Composable
fun CloseProfile(closeProfile: () -> Unit) {
    DI.get<CloseProfileView>().create(closeProfile)
}

class CloseProfileViewImpl : CloseProfileView {
    @Composable
    override fun create(closeProfile: () -> Unit) {
        val i18n = DI.get<I18nView>()

        Box {
            Tooltip({ Text(i18n.accountCloseProfile()) }) {
                ThemedIconButton(style = MaterialTheme.components.destructiveIconButton, onClick = { closeProfile() }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, i18n.accountCloseProfile())
                }
            }
        }
    }
}
