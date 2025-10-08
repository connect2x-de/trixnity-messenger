package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.roomlist.room.RoomListElement
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListElementViewModel4
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomListElementPreview() {
    InitMessengerPreview {
        val roomElementViewModel = PreviewRoomListElementViewModel4()
        roomElementViewModel.isInvite.value = true
        roomElementViewModel.roomName.value = "Invitation into 'Looong group name'"
        roomElementViewModel.lastMessage.value = "from Martin"
        roomElementViewModel.error.value = "Oh no!"
        roomElementViewModel.time.value = null
        RoomListElement(
            roomListElementViewModel = roomElementViewModel,
            roomListViewModel = PreviewRoomListViewModel()
        )
    }
}
