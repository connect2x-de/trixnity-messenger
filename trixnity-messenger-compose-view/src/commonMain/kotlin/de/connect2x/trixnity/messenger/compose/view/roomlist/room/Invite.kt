package de.connect2x.trixnity.messenger.compose.view.roomlist.room

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedProgressIndicator
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedSelectableText
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListElementViewModel

interface InviteRoomListElement {
    @Composable
    fun create(roomListElementViewModel: RoomListElementViewModel, index: Int)
}

@Composable
fun Invite(
    roomListElementViewModel: RoomListElementViewModel,
    index: Int,
) {
    DI.get<InviteRoomListElement>().create(roomListElementViewModel, index)
}

class InviteRoomListElementImpl : InviteRoomListElement {
    @Composable
    override fun create(roomListElementViewModel: RoomListElementViewModel, index: Int) {
        val i18n = DI.get<I18nView>()
        var showReject by remember { mutableStateOf(false) }
        val roomName = roomListElementViewModel.roomName.collectAsState().value
        val inviterUserInfo = roomListElementViewModel.inviterUserInfo.collectAsState().value

        RoomComponent(
            roomListElementViewModel,
            roomDetails = {
                RoomName(roomName)
                inviterUserInfo?.let { inviterUserInfo ->
                    RoomInviterUserInfo(inviterNameOrUserId = inviterUserInfo.name)
                    RoomInviterUserInfo(inviterNameOrUserId = inviterUserInfo.userId.full)
                }

            },
            roomActions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tooltip(tooltip = i18n.invitationAccept()) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = { roomListElementViewModel.acceptInvitation() },
                        ) {
                            Icon(Icons.Default.Check, i18n.invitationAccept())
                        }
                    }
                    Tooltip(tooltip = i18n.invitationReject()) {
                        ThemedIconButton(
                            style = MaterialTheme.components.commonIconButton,
                            onClick = { showReject = true },
                        ) {
                            Icon(Icons.Default.Close, i18n.invitationReject())
                        }
                    }
                }
            },
            index
        )

        if (showReject) {
            val rejectionInProgress = roomListElementViewModel.rejectInvitationInProgress.collectAsState().value
            ThemedModalDialog({ showReject = false }) {
                ModalDialogHeader {
                    Text(i18n.invitationRejectHeader())
                }
                ModalDialogContent {
                    if (rejectionInProgress) {
                        ThemedProgressIndicator(style = MaterialTheme.components.circularProgressIndicator)
                        return@ModalDialogContent
                    }
                    ThemedSelectableText(
                        text = i18n.formattedInvitationBody(
                            inviterName = inviterUserInfo?.name ?: i18n.commonUnknown(),
                            roomName = roomName,
                        ),
                        selectionStyle = MaterialTheme.components.selectionOnSurface,
                    )
                }
                ModalDialogFooter {
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = { showReject = false },
                        enabled = !rejectionInProgress
                    ) {
                        Text(i18n.actionCancel())
                    }
                    ThemedButton(
                        style = MaterialTheme.components.commonButton,
                        onClick = { roomListElementViewModel.rejectInvitationAndBlockInviter() },
                        enabled = !rejectionInProgress
                    ) {
                        Text(i18n.invitationBlock())
                    }
                    ThemedButton(
                        style = MaterialTheme.components.primaryButton,
                        onClick = { roomListElementViewModel.rejectInvitation() },
                        enabled = !rejectionInProgress
                    ) {
                        Text(i18n.invitationReject())
                    }
                }
            }
        }
    }

    @Composable
    fun RoomInviterUserInfo(inviterNameOrUserId: String) {
        val needsTooltip = remember { mutableStateOf(false) }
        Tooltip(
            enabled = needsTooltip.value,
            tooltip = { Text(inviterNameOrUserId) }
        ) {
            Text(
                inviterNameOrUserId,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = {
                    needsTooltip.value = it.hasVisualOverflow
                }
            )
        }
    }
}
