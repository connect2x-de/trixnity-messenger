package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.util.rovingFocusChild
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface KnockRoomListElement {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun Knock(
    roomListElementViewModel: RoomListElementViewModel,
) {
    DI.get<KnockRoomListElement>().create(roomListElementViewModel)
}

class KnockRoomListElementImpl : KnockRoomListElement {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        val i18n = DI.get<I18nView>()

        SpecialRoomComponent(
            roomListElementViewModel = roomListElementViewModel,
        ) {
            ThemedIconButton(
                onClick = { roomListElementViewModel.unknock() },
                modifier = Modifier.Companion.rovingFocusChild(),
            ) {
                Icon(Icons.Default.Close, i18n.unknock())
            }
        }
    }
}
