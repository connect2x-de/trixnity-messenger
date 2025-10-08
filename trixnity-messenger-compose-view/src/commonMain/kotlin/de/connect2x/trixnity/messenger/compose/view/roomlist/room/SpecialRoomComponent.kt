package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun SpecialRoomComponent(
    roomListElementViewModel: RoomListElementViewModel,
    extraInfo: @Composable ColumnScope.() -> Unit = {},
    buttons: @Composable RowScope.() -> Unit
) {
    val roomName = roomListElementViewModel.roomName.collectAsState().value

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.fillMaxWidth().weight(1.0f, false)) {
            RoomName(roomName = roomName)
            extraInfo()
        }
        Spacer(Modifier.size(10.dp))
        buttons()
    }
}
