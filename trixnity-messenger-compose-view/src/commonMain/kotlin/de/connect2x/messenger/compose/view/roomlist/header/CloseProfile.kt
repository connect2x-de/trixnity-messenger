package de.connect2x.messenger.compose.view.roomlist.header

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.TooltipText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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
                Tooltip({ TooltipText(i18n.accountCloseProfile()) }) {
                    IconButton(
                        onClick = {
                            roomListViewModel.closeProfile()
                        },
                        modifier = Modifier.buttonPointerModifier(),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Close")
                    }
                }
            }
        }
    }
}
