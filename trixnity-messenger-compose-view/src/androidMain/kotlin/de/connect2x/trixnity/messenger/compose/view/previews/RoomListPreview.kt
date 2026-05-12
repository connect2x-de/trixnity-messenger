package de.connect2x.trixnity.messenger.compose.view.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.trixnity.messenger.compose.view.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.compose.view.roomlist.RoomList
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomListPreview() {
    InitMessengerPreview {
        RoomList(roomListViewModel = PreviewRoomListViewModel())
    }
}
