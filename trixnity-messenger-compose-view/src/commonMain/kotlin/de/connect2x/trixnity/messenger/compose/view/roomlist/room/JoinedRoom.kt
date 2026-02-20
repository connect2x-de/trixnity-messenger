package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface JoinedRoomListView {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun JoinedRoom(roomListElementViewModel: RoomListElementViewModel) {
    DI.current.get<JoinedRoomListView>().create(roomListElementViewModel)
}

class JoinedRoomListViewImpl : JoinedRoomListView {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        Row(
            Modifier.background(Color.Red).fillMaxWidth()
        ) {
            RoomNameAndLastMessage(roomListElementViewModel)
            RoomTimeAndUnreadMessagesCounter(roomListElementViewModel)
        }
    }
}
