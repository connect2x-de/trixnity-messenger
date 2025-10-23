package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MapsUgc
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.icons.PublicIcon
import de.connect2x.messenger.compose.view.common.modifier.PlaceholderHighlight
import de.connect2x.messenger.compose.view.common.modifier.fade
import de.connect2x.messenger.compose.view.common.placeholder
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.AvatarContentIcon
import de.connect2x.messenger.compose.view.theme.components.AvatarPresenceBadge
import de.connect2x.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.messenger.compose.view.theme.components.ThemedAvatar
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.messenger.compose.view.theme.components.ThemedUserAvatar
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListViewModel
import de.connect2x.trixnity.messenger.viewmodel.util.avatarSize

interface RoomListElementView {
    @Composable
    fun create(roomListViewModel: RoomListViewModel, roomListElementViewModel: RoomListElementViewModel, index: Int)
}

@Composable
private fun ErrorModalDialog(error: String, onDismiss: () -> Unit) {
    val i18n = DI.get<I18nView>()
    ThemedModalDialog(onDismissRequest = onDismiss) {
        ModalDialogHeader {
            Text(i18n.commonError())
        }
        ModalDialogContent {
            Text(error)
        }
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
) {
    DI.get<RoomListElementView>().create(roomListViewModel, roomListElementViewModel, index)
}

class RoomListElementViewImpl : RoomListElementView {
    @Composable
    override fun create(
        roomListViewModel: RoomListViewModel,
        roomListElementViewModel: RoomListElementViewModel,
        index: Int
    ) {
        val isInvite = roomListElementViewModel.isInvite.collectAsState().value
        val isLeave = roomListElementViewModel.isLeave.collectAsState().value
        val isLoaded = roomListElementViewModel.isLoaded.collectAsState().value
        val isKnock = roomListElementViewModel.isKnock.collectAsState().value == true
        val error by roomListElementViewModel.error.collectAsState()
        val roomName = roomListElementViewModel.roomName.collectAsState().value
        error?.let { ErrorModalDialog(it, roomListElementViewModel::clearError) }

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
//            ListItem() // FIXME use this to get correct a11y behavior?
            MatrixClientColor(roomListElementViewModel)
            Row(
                Modifier
                    .heightIn(min = 72.dp)
                    .padding(top = 10.dp, bottom = 10.dp, end = 10.dp)
                    .placeholder(
                        visible = !isLoaded,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(8.dp),
                        highlight = PlaceholderHighlight.fade(highlightColor = Color(0xFFDDDDDD))
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoomImage(roomListElementViewModel)
                Spacer(Modifier.size(10.dp))

                when {
                    isInvite == true -> Invite(roomListElementViewModel)
                    isLeave == true -> ArchivedRoom(roomListElementViewModel)
                    isKnock == true -> Knock(roomListElementViewModel)
                    else -> Column(
                        Modifier.align(Alignment.CenterVertically)
                            .clearAndSetSemantics {
                                text = AnnotatedString(roomName ?: "")
                                role = Role.Button
                                collectionItemInfo = CollectionItemInfo(
                                    rowIndex = index,
                                    rowSpan = 1,
                                    columnIndex = 0,
                                    columnSpan = 0,
                                )
                            }) {
                        RoomNameAndTime(roomListElementViewModel)
                        LastMessageAndUnreadMessagesCounter(roomListElementViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixClientColor(roomElementViewModel: RoomListElementViewModel) {
    val accountColor = roomElementViewModel.accountColor.collectAsState().value
    Box(Modifier.padding(start = 2.dp, end = 12.dp).fillMaxHeight().padding(vertical = 0.dp)) {
        Box(
            Modifier
                .width(if (accountColor != null) 6.dp else 0.dp)
                .fillMaxHeight()
                .background(if (accountColor != null) Color(accountColor) else Color.Transparent)
        )
    }
}

@Composable
fun RoomImage(roomElementViewModel: RoomListElementViewModel) {
    val i18n = DI.get<I18nView>()
    val isInvite = roomElementViewModel.isInvite.collectAsState().value
    val roomImage = roomElementViewModel.roomImage.collectAsState().value
    val roomImageInitials = roomElementViewModel.roomImageInitials.collectAsState().value
    val presence = roomElementViewModel.presence.collectAsState().value
    val isPublic = roomElementViewModel.isPublic.collectAsState().value ?: false
    if (isInvite == null || roomImageInitials == null) {
        Box(
            Modifier
                .width(avatarSize().dp)
                .height(avatarSize().dp)
                .placeholder(
                    visible = true,
                    color = Color.LightGray,
                    shape = CircleShape,
                )
        )
    } else {
        if (isInvite) {
            ThemedAvatar(avatarSize().dp) {
                AvatarContentIcon(Icons.Default.MapsUgc, avatarSize().dp)
            }
        } else {
            Box {
                ThemedUserAvatar(roomImageInitials, roomImage, presence) {
                    AvatarPresenceBadge(presence)
                }
                if (isPublic) PublicIcon()
            }
        }
    }
}
