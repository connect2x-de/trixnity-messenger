package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.Tooltip
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel

interface CloseProfileView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel)
}

@Composable
fun CloseProfile(roomListViewModel: RoomListViewModel) {
    DI.get<CloseProfileView>().create(roomListViewModel)
}

class CloseProfileViewImpl : CloseProfileView {
    @Composable
    override fun create(roomListViewModel: RoomListViewModel) {
        val i18n = DI.get<I18nView>()

        if (roomListViewModel.closeProfileNeeded) {
            Box {
                Tooltip({ Text(i18n.accountCloseProfile()) }) {
                    ThemedIconButton(
                        style = MaterialTheme.components.destructiveIconButton,
                        onClick = { roomListViewModel.closeProfile() },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, i18n.accountCloseProfile())
                    }
                }
            }
        }
    }
}
