package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MarkAsUnread
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.room.settings.RoomLeaveWarning
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenuItem
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
internal fun RoomListElementViewModel.RoomListElementContextMenuActions(i18n: I18nView): List<ThemedActionMenuItem> {
    val isUnread = isUnread.collectAsState().value ?: false
    val roomI18n = DI.get<I18n>()
    val isDirect = isDirect.collectAsState().value == true
    var showLeaveWarning by remember(roomId) { mutableStateOf(false) }
    if (showLeaveWarning) {
        RoomLeaveWarning(
            isDirect = isDirect,
            onDismiss = { showLeaveWarning = false },
            onConfirm = {
                leaveRoom()
                showLeaveWarning = false
            },
            onForget = {
                forgetRoom()
                showLeaveWarning = false
            },
        )
    }
    return buildList {
        if (!isUnread)
            add(ThemedActionMenuItem(Icons.Default.MarkAsUnread, i18n.markRoomAsUnread(), action = { markUnread() }))
        if (isUnread)
            add(ThemedActionMenuItem(Icons.Default.MarkChatRead, i18n.markRoomAsRead(), action = { markRead() }))
        add(
            ThemedActionMenuItem(
                Icons.AutoMirrored.Filled.Logout,
                if (isDirect) roomI18n.settingsRoomLeaveRoomMessageChat()
                else roomI18n.settingsRoomLeaveRoomMessageGroup(),
                action = { showLeaveWarning = true },
                contentColor = MaterialTheme.colorScheme.error,
            )
        )
    }
}
