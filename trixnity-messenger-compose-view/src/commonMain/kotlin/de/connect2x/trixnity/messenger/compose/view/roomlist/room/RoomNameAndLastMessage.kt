package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun RoomName(roomName: String?, modifier: Modifier = Modifier) {
    Tooltip({ Text(roomName ?: " ") }) {
        Row(modifier) {
            Text( // cannot be SelectableText as this will require focus and tabbing through the room list is painful
                text = roomName ?: " ",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun RoomTime(roomListElementViewModel: RoomListElementViewModel, modifier: Modifier = Modifier) {
    val time = roomListElementViewModel.time.collectAsState().value
    Row { Text(time ?: " ", style = MaterialTheme.typography.labelMedium, maxLines = 1) }
}

@Composable
fun ColumnScope.RoomNameAndLastMessage(roomListElementViewModel: RoomListElementViewModel) {
    val roomName = roomListElementViewModel.roomName.collectAsState().value
    val lastMessage = roomListElementViewModel.lastMessage.collectAsState().value
    val usersTyping = roomListElementViewModel.usersTyping.collectAsState().value

    RoomName(roomName)
    Tooltip({
        Box(Modifier.widthIn(0.dp, 300.dp)) {
            Text(usersTyping ?: lastMessage ?: " ", maxLines = 5, overflow = TextOverflow.Ellipsis)
        }
    }) {
        LastMessage(lastMessage, usersTyping)
    }
}
