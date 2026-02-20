package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.icons.UnencryptedIcon
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun RoomName(roomName: String?, modifier: Modifier = Modifier) {
    Tooltip({ Text(roomName ?: " ") }) {
        Row(modifier) {
            Text( // cannot be SelectableText as this will require focus and tabbing through the room list is painful
                text = roomName ?: " ",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RoomTime(roomListElementViewModel: RoomListElementViewModel, modifier: Modifier = Modifier) {
    val time = roomListElementViewModel.time.collectAsState().value
    val isEncrypted = roomListElementViewModel.isEncrypted.collectAsState().value
    Row {
        if (isEncrypted != null && isEncrypted.not()) {
            Box(Modifier.padding(end = 5.dp), contentAlignment = Alignment.Center) {
                UnencryptedIcon()
            }
        }
        Text(
            time ?: " ",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
fun RowScope.RoomNameAndLastMessage(roomListElementViewModel: RoomListElementViewModel) {
    val roomName = roomListElementViewModel.roomName.collectAsState().value
    val lastMessage = roomListElementViewModel.lastMessage.collectAsState().value
    val usersTyping = roomListElementViewModel.usersTyping.collectAsState().value

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.weight(1f, true)
    ) {
        RoomName(roomName = roomName)
        LastMessage(lastMessage, usersTyping)
    }
}
