package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface JoinedRoomListView {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel, index: Int, showActions: Boolean)
}

@Composable
fun JoinedRoom(roomListElementViewModel: RoomListElementViewModel, index: Int, showActions: Boolean) {
    DI.current.get<JoinedRoomListView>().create(roomListElementViewModel, index, showActions)
}

class JoinedRoomListViewImpl : JoinedRoomListView {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel, index: Int, showActions: Boolean) {

        val actionVisibility = animateFloatAsState(targetValue = if (showActions) 1f else 0f).value
        RoomComponent(
            roomListElementViewModel,
            roomDetails = { RoomNameAndLastMessage(roomListElementViewModel) },
            roomActions = {
                Box(Modifier.alpha(actionVisibility)) {
                    RoomTimeAndUnreadMessagesCounter(roomListElementViewModel)
                }
            },
            index
        )
    }
}
