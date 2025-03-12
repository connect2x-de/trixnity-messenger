package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface KnockView {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun Knock(
    roomListElementViewModel: RoomListElementViewModel,
) {
    DI.get<KnockView>().create(roomListElementViewModel)
}

class KnockViewImpl : KnockView {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        val i18n = DI.get<I18nView>()
        val roomName = roomListElementViewModel.roomName.collectAsState().value

        Row(verticalAlignment = Alignment.CenterVertically) {
            RoomName(roomName = roomName)

            Spacer(Modifier.size(10.dp))

            IconButton(
                onClick = { roomListElementViewModel.unknock() },
                modifier = Modifier.buttonPointerModifier(),
            ) {
                Icon(Icons.Default.Close, i18n.invitationReject())
            }
        }
    }
}
