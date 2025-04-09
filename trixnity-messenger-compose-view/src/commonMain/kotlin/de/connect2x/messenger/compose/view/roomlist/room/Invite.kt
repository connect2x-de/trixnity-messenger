package de.connect2x.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.Tooltip
import de.connect2x.messenger.compose.view.common.MessengerModal
import de.connect2x.messenger.compose.view.common.MessengerModalContent
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface InviteRoomListElement {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel)
}

@Composable
fun Invite(
    roomListElementViewModel: RoomListElementViewModel,
) {
    DI.get<InviteRoomListElement>().create(roomListElementViewModel)
}

class InviteRoomListElementImpl : InviteRoomListElement {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel) {
        val i18n = DI.get<I18nView>()
        var showReject by remember { mutableStateOf(false) }
        val roomName = roomListElementViewModel.roomName.collectAsState().value
        val inviterUserInfo = roomListElementViewModel.inviterUserInfo.collectAsState().value

        SpecialRoomComponent(
            roomListElementViewModel = roomListElementViewModel,
            extraInfo = {
                inviterUserInfo?.let { inviterUserInfo ->
                    RoomInviterUserInfo(inviterNameOrUserId = inviterUserInfo.name)
                    RoomInviterUserInfo(inviterNameOrUserId = inviterUserInfo.userId.full)
                }
            }
        ) {
            Tooltip(
                tooltip = { Text(i18n.invitationAccept())}
            ) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { roomListElementViewModel.acceptInvitation() },
                ) {
                    Icon(Icons.Default.Check, i18n.invitationAccept())
                }
            }
            Tooltip(
                tooltip = { Text(i18n.invitationReject())}
            ) {
                ThemedIconButton(
                    style = MaterialTheme.components.commonIconButton,
                    onClick = { showReject = true },
                ) {
                    Icon(Icons.Default.Close, i18n.invitationReject())
                }
            }
        }

        if (showReject) {
            Dialog(onDismissRequest = { showReject = false }) {
                MessengerModal(
                    onDismiss = { showReject = false },
                    width = 500.dp,
                    title = i18n.invitationRejectHeader(),
                ) {
                    MessengerModalContent {
                        Text(
                            text = i18n.formattedInvitationBody(
                                inviterName = inviterUserInfo?.name ?: i18n.commonUnknown(),
                                roomName = roomName,
                            ),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.size(20.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(IntrinsicSize.Min)
                        ) {
                            Box(Modifier.weight(1.0f, fill = true), contentAlignment = Alignment.Center) {
                                ThemedButton(
                                    style = MaterialTheme.components.primaryButton,
                                    onClick = { roomListElementViewModel.rejectInvitation() },
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Text(i18n.invitationReject())
                                }
                            }
                            Spacer(Modifier.size(20.dp))
                            Box(Modifier.weight(1.0f, fill = true), contentAlignment = Alignment.Center) {
                                ThemedButton(
                                    style = MaterialTheme.components.commonButton,
                                    onClick = { roomListElementViewModel.rejectInvitationAndBlockInviter() },
                                ) {
                                    Text(i18n.invitationBlock())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RoomInviterUserInfo(inviterNameOrUserId: String) {
        Text(
            inviterNameOrUserId,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}
