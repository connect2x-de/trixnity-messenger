package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface JoinedRoomListView {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel, index: Int)
}

@Composable
fun JoinedRoom(roomListElementViewModel: RoomListElementViewModel, index: Int) {
    DI.current.get<JoinedRoomListView>().create(roomListElementViewModel, index)
}

class JoinedRoomListViewImpl : JoinedRoomListView {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel, index: Int) {
        RoomComponent(
            roomListElementViewModel,
            roomDetails = { RoomNameAndLastMessage(roomListElementViewModel) },
            roomActions = { RoomTimeAndUnreadMessagesCounter(roomListElementViewModel) },
            index
        )
    }
}
