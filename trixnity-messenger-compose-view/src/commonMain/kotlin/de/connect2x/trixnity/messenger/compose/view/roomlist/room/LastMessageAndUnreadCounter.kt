package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.theme.dp
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun LastMessageAndUnreadMessagesCounter(roomListElementViewModel: RoomListElementViewModel) {
    val lastMessage = roomListElementViewModel.lastMessage.collectAsState().value
    val usersTyping = roomListElementViewModel.usersTyping.collectAsState().value
    val isUnread = roomListElementViewModel.isUnread.collectAsState().value
    val notificationCount = roomListElementViewModel.notificationCount.collectAsState().value

    Tooltip({ Text(usersTyping ?: lastMessage ?: " ") }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().weight(1.0f, false).alignByBaseline()) {
                LastMessage(lastMessage, usersTyping)
            }
            val size = MaterialTheme.typography.labelSmall.dp
            when {
                notificationCount != null -> {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier
                            .defaultMinSize(minWidth = size)
                            .height(size)
                            .alignByBaseline(),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            notificationCount,
                            Modifier.padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                        )
                    }
                }

                isUnread == true -> {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier
                            .size(size)
                            .alignByBaseline(),
                        color = MaterialTheme.colorScheme.primary,
                    ) {}
                }
            }
        }
    }
}


@Composable
fun LastMessage(lastMessage: String?, usersTyping: String?) {
    if (usersTyping != null) {
        Text(
            usersTyping,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Text(
            lastMessage ?: " ",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
