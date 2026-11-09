package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.abi.TrixnityMessengerCallApi
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.icons.UnencryptedIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

/**
 * Symbols are shown in the room list for each item. Standard implementation only shows unencrypted icon if room is not
 * encrypted.
 */
interface RoomListElementSymbolsView {
    @Composable fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun RoomListElementSymbols(roomListElementViewModel: RoomListElementViewModel) {
    with(DI.get<RoomListElementSymbolsView>()) { create(roomListElementViewModel) }
}

class RoomListElementSymbolsViewImpl : RoomListElementSymbolsView {
    @Composable
    @OptIn(TrixnityMessengerCallApi::class)
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        val isEncrypted = roomListElementViewModel.isEncrypted.collectAsState().value
        val hasRunningCall = roomListElementViewModel.hasRunningCall.collectAsState().value
        val i18n = DI.get<I18nView>()

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (hasRunningCall) {
                Tooltip({ Text(i18n.roomListRunningCall()) }) {
                    Icon(
                        Icons.Default.Videocam,
                        i18n.roomListRunningCall(),
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isEncrypted != null && isEncrypted.not()) {
                UnencryptedIcon()
            }
        }
    }
}
