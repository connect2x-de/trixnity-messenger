package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.common.PlaceholderHighlight
import de.connect2x.messenger.compose.view.common.fade
import de.connect2x.messenger.compose.view.common.placeholder
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun LastMessageAndUnreadMessagesCounter(roomListElementViewModel: RoomListElementViewModel) {
    val lastMessage = roomListElementViewModel.lastMessage.collectAsState().value
    val usersTyping = roomListElementViewModel.usersTyping.collectAsState().value
    val unreadMessages = roomListElementViewModel.unreadMessages.collectAsState().value

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .placeholder(
                visible = lastMessage == null,
                color = Color.LightGray,
                shape = RoundedCornerShape(8.dp),
                highlight = PlaceholderHighlight.fade(highlightColor = Color(0xFFDDDDDD))
            )
    ) {
        Box(Modifier.fillMaxWidth().weight(1.0f, false).alignByBaseline()) {
            LastMessage(lastMessage, usersTyping)
        }
        if (unreadMessages != null) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.alignByBaseline(),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    unreadMessages,
                    Modifier.padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
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
