package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
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
        val roomName = roomListElementViewModel.roomName.collectAsState().value

        RoomName(roomName = roomName)
    }
}
