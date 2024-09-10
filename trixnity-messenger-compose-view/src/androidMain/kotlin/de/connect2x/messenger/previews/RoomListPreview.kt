package de.connect2x.messenger.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.connect2x.messenger.compose.view.roomlist.RoomList
import de.connect2x.messenger.previews.util.InitMessengerPreview
import de.connect2x.trixnity.messenger.viewmodel.roomlist.PreviewRoomListViewModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFF)
@Composable
private fun RoomListPreview() {
    InitMessengerPreview {
        RoomList(roomListViewModel = PreviewRoomListViewModel())
    }
}
