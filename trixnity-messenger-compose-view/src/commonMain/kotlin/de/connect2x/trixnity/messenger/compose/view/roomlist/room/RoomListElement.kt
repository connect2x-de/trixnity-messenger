package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MapsUgc
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.icons.PublicIcon
import de.connect2x.trixnity.messenger.compose.view.common.placeholder
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarContentIcon
import de.connect2x.trixnity.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedAvatar
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize

interface RoomListElementView {
    @Composable
    fun create(
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel,
        index: Int,
        showRoomTime: Boolean,
    )
}

@Composable
private fun ErrorModalDialog(error: String, onDismiss: () -> Unit) {
    val i18n = DI.get<I18nView>()
    ThemedModalDialog(onDismissRequest = onDismiss) {
        ModalDialogHeader { Text(i18n.commonError()) }
        ModalDialogContent { Text(error) }
        ModalDialogFooter {
            ThemedButton(style = MaterialTheme.components.primaryButton, onClick = onDismiss) {
                Text(i18n.commonClose())
            }
        }
    }
}

@Composable
fun RoomListElement(
    roomListViewModel: RoomListViewModel,
    roomListElementViewModel: RoomListElementViewModel,
    index: Int,
    showRoomTime: Boolean,
) {
    DI.get<RoomListElementView>().create(roomListViewModel, roomListElementViewModel, index, showRoomTime)
}

class RoomListElementViewImpl : RoomListElementView {
    @Composable
    override fun create(
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel,
        index: Int,
        showRoomTime: Boolean,
    ) {
        val isInvite = roomListElementViewModel.isInvite.collectAsState().value == true
        val isLeave = roomListElementViewModel.isLeave.collectAsState().value == true
        val isKnock = roomListElementViewModel.isKnock.collectAsState().value == true
        val error by roomListElementViewModel.error.collectAsState()
        roomListElementViewModel.roomName.collectAsState().value
        error?.let { ErrorModalDialog(it, roomListElementViewModel::clearError) }
        Box(
            Modifier.semantics {
                role = Role.Button
                collectionItemInfo = CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
            }
        ) {
            when {
                isInvite -> Invite(roomListElementViewModel)
                isLeave -> ArchivedRoom(roomListElementViewModel)
                isKnock -> Knock(roomListElementViewModel)
                else -> JoinedRoom(roomListElementViewModel, showRoomTime)
            }
        }
    }
}

@Composable
fun MatrixClientColor(roomElementViewModel: RoomListElementViewModel) {
    val accountColor = roomElementViewModel.accountColor.collectAsState().value
    Box(Modifier.padding(start = 2.dp, end = 12.dp).fillMaxHeight().padding(vertical = 0.dp)) {
        Box(
            Modifier.width(if (accountColor != null) 6.dp else 0.dp)
                .fillMaxHeight()
                .background(if (accountColor != null) Color(accountColor) else Color.Transparent)
        )
    }
}

@Composable
fun RoomImage(roomElementViewModel: RoomListElementViewModel) {
    DI.get<I18nView>()
    val isInvite = roomElementViewModel.isInvite.collectAsState().value
    val roomImage = roomElementViewModel.roomImage.collectAsState().value
    val roomImageInitials = roomElementViewModel.roomImageInitials.collectAsState().value
    val presence = roomElementViewModel.presence.collectAsState().value
    val isPublic = roomElementViewModel.isPublic.collectAsState().value ?: false
    if (isInvite == null || roomImageInitials == null) {
        Box(
            Modifier.width(avatarSize().dp)
                .height(avatarSize().dp)
                .placeholder(visible = true, color = Color.LightGray, shape = CircleShape)
        )
    } else {
        if (isInvite) {
            ThemedAvatar(avatarSize().dp) { AvatarContentIcon(Icons.Default.MapsUgc, avatarSize().dp) }
        } else {
            Box {
                ThemedUserAvatar(roomImageInitials, roomImage, presence) { AvatarPresenceBadge(presence) }
                if (isPublic) PublicIcon()
            }
        }
    }
}
