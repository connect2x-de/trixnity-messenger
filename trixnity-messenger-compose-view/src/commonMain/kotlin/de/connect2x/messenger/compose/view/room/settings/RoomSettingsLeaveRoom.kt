package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.WarningDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsViewModel

interface RoomSettingsLeaveRoomView {
    @Composable
    fun create(roomSettingsViewModel: RoomSettingsViewModel)
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
            OutlinedButton(
                onClick = { roomSettingsViewModel.openLeaveRoomWarningDialog() },
                modifier = Modifier.buttonPointerModifier(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, leaveRoomSettingEntryText)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = leaveRoomSettingEntryText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun RoomSettingsLeaveRoomWarning(roomSettingsViewModel: RoomSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    val hasLeft = roomSettingsViewModel.hasLeft.collectAsState().value
    val leaveRoomWarningMessage = roomSettingsViewModel.leaveRoomWarningMessage.collectAsState().value
    val leaveRoomWarningTitle = roomSettingsViewModel.leaveRoomWarningTitle.collectAsState().value
    val leaveRoomWarningConfirmButtonText =
        roomSettingsViewModel.leaveRoomWarningConfirmButtonText.collectAsState().value

    WarningDialog(
        title = leaveRoomWarningTitle,
        message = { Text(leaveRoomWarningMessage) },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = leaveRoomWarningConfirmButtonText,
        dismissAction = { roomSettingsViewModel.closeLeaveRoomWarningDialog() },
        confirmAction = {
            if (hasLeft) {
                roomSettingsViewModel.forgetRoom()
            } else {
                roomSettingsViewModel.leaveRoom()
            }
            roomSettingsViewModel.closeLeaveRoomWarningDialog()
        }
    )
}
