package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.runtime.Composable
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface JoinedRoomListView {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel, index: Int, showRoomTime: Boolean)
}

@Composable
fun JoinedRoom(roomListElementViewModel: RoomListElementViewModel, index: Int, showRoomTime: Boolean) {
    DI.current.get<JoinedRoomListView>().create(roomListElementViewModel, index, showRoomTime)
}

class JoinedRoomListViewImpl : JoinedRoomListView {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel, index: Int, showRoomTime: Boolean) {

        RoomListElementBase(
            roomListElementViewModel,
            roomDetails = { RoomNameAndLastMessage(roomListElementViewModel) },
            roomActions = {
                RoomTimeAndUnreadMessagesCounter(roomListElementViewModel, showRoomTime)
            },
            index
        )
    }
}
