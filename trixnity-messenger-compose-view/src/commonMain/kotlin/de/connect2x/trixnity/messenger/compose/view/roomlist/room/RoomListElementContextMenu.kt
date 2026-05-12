package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkAsUnread
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedActionMenuItem
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

@Composable
internal fun RoomListElementViewModel.RoomListElementContextMenuActions(i18n: I18nView): List<ThemedActionMenuItem> {
    val isUnread = isUnread.collectAsState().value ?: false
    return buildList {
        if (!isUnread) add(
            ThemedActionMenuItem(
                Icons.Default.MarkAsUnread,
                i18n.markRoomAsUnread(),
                action = { markUnread() })
        )
        if (isUnread) add(
            ThemedActionMenuItem(
                Icons.Default.MarkChatRead,
                i18n.markRoomAsRead(),
                action = { markRead() }
            )
        )
    }
}
