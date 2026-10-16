package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsLeaveRoomView {
    @Composable fun create(roomSettingsViewModel: RoomSettingsViewModel)
}

@Composable
fun RoomSettingsLeaveRoom(roomSettingsViewModel: RoomSettingsViewModel) {
    DI.get<RoomSettingsLeaveRoomView>().create(roomSettingsViewModel)
}

class RoomSettingsLeaveRoomViewImpl : RoomSettingsLeaveRoomView {
    @Composable
    override fun create(roomSettingsViewModel: RoomSettingsViewModel) {
        val leaveRoomSettingEntryText = roomSettingsViewModel.leaveRoomSettingEntryText.collectAsState().value
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemedButton(
                style = MaterialTheme.components.destructiveButton,
                onClick = { roomSettingsViewModel.openLeaveRoomWarningDialog() },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    leaveRoomSettingEntryText,
                    modifier = Modifier.size(MaterialTheme.components.destructiveButton.iconSize),
                )
                Spacer(Modifier.size(MaterialTheme.components.destructiveButton.iconSpacing))
                Text(text = leaveRoomSettingEntryText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun RoomSettingsLeaveRoomWarning(roomSettingsViewModel: RoomSettingsViewModel) {
    val isLeave = roomSettingsViewModel.isLeave.collectAsState().value
    RoomLeaveWarning(
        isDirect = roomSettingsViewModel.isDirect.collectAsState().value,
        isLeave = isLeave,
        onDismiss = roomSettingsViewModel::closeLeaveRoomWarningDialog,
        onConfirm = {
            roomSettingsViewModel.leaveRoom()
            roomSettingsViewModel.closeLeaveRoomWarningDialog()
        },
        onForget = {
            roomSettingsViewModel.forgetRoom()
            roomSettingsViewModel.closeLeaveRoomWarningDialog()
        },
    )
}

@Composable
internal fun RoomLeaveWarning(
    isDirect: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onForget: () -> Unit,
    isLeave: Boolean = false,
) {
    val i18n = DI.get<I18nView>()
    val roomI18n = DI.get<I18n>()
    val title =
        when {
            isLeave && isDirect -> roomI18n.settingsRoomForgetRoomWarningTitleChat()
            isLeave -> roomI18n.settingsRoomForgetRoomWarningTitleGroup()
            isDirect -> roomI18n.settingsRoomLeaveRoomWarningTitleChat()
            else -> roomI18n.settingsRoomLeaveRoomWarningTitleGroup()
        }
    val message =
        if (isDirect) roomI18n.settingsRoomLeaveRoomWarningMessageChat()
        else roomI18n.settingsRoomLeaveRoomWarningMessageGroup()
    val confirm =
        when {
            isLeave && isDirect -> roomI18n.settingsRoomForgetRoomWarningConfirmButtonChat()
            isLeave -> roomI18n.settingsRoomForgetRoomWarningConfirmButtonGroup()
            isDirect -> roomI18n.settingsRoomLeaveRoomWarningConfirmButtonChat()
            else -> roomI18n.settingsRoomLeaveRoomWarningConfirmButtonGroup()
        }
    ThemedModalDialog(onDismiss) {
        ModalDialogHeader { Text(title) }
        ModalDialogContent { Text(message) }
        ModalDialogFooter {
            ThemedButton(style = MaterialTheme.components.commonButton, onClick = onDismiss) {
                Text(i18n.actionCancel())
            }
            if (!isLeave) {
                ThemedButton(style = MaterialTheme.components.primaryButton, onClick = onConfirm) { Text(confirm) }
            }
            ThemedButton(style = MaterialTheme.components.destructiveButton, onClick = onForget) {
                Text(if (isLeave) confirm else i18n.leaveAndForgetRoom())
            }
        }
    }
}
