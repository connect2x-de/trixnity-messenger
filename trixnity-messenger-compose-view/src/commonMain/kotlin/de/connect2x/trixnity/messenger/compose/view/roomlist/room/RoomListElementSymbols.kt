package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.icons.UnencryptedIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

/**
 * Symbols are shown in the room list for each item. Standard implementation only shows unencrypted icon if room is
 * not encrypted.
 */
interface RoomListElementSymbolsView {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun RoomListElementSymbols(roomListElementViewModel: RoomListElementViewModel) {
    with(DI.get<RoomListElementSymbolsView>()) { create(roomListElementViewModel) }
}

class RoomListElementSymbolsViewImpl : RoomListElementSymbolsView {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        val isEncrypted = roomListElementViewModel.isEncrypted.collectAsState().value

        if (isEncrypted != null && isEncrypted.not()) {
            Box(contentAlignment = Alignment.Center) {
                UnencryptedIcon()
            }
        }
    }
}

