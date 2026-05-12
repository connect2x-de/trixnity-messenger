package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.NotificationAndUnreadMarker
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.VerySmallSpacer
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
fun RoomTimeAndUnreadMessagesCounter(roomListElementViewModel: RoomListElementViewModel, showRoomTime: Boolean = true) {
    val isUnread = roomListElementViewModel.isUnread.collectAsState().value
    val notificationCount = roomListElementViewModel.notificationCount.collectAsState().value
    val actionVisibility = animateFloatAsState(targetValue = if (showRoomTime) 1f else 0f).value
    val i18n = DI.current.get<I18nView>()
    Tooltip(
        {
            when {
                notificationCount != null -> Text(i18n.unreadMessageCount(notificationCount))
                isUnread == true -> Text(i18n.indicatorUnreadMessages())
            }
        },
        modifier = Modifier,
        enabled = notificationCount != null || isUnread == true,
    ) {
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.End
        ) {
            Row {
                RoomListElementSymbols(roomListElementViewModel)
                VerySmallSpacer()
                Box(Modifier.alpha(actionVisibility)) {
                    RoomTime(roomListElementViewModel)
                }
            }
            VerySmallSpacer()
            NotificationAndUnreadMarker(notificationCount, isUnread)
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
