package de.connect2x.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.WarningDialog
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.settings.Setting
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsSecurityViewModel

interface RoomSettingsSecurityView {
    @Composable
    fun create(roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel)
}

@Composable
fun RoomSettingsSecurity(roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel) {
    DI.get<RoomSettingsSecurityView>().create(roomSettingsSecurityViewModel)
}

class RoomSettingsSecurityViewImpl : RoomSettingsSecurityView {
    @Composable
    override fun create(roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel) {
        val i18n = DI.get<I18nView>()
        val isEncrypting = roomSettingsSecurityViewModel.isEncrypted.collectAsState().value
        val canEnableEncryption = roomSettingsSecurityViewModel.canEnableEncryption.collectAsState().value
        val leaveEnableEncryptionWarningOpen =
            roomSettingsSecurityViewModel.isEncryptionWarningOpen.collectAsState().value

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = i18n.security().capitalize(Locale.current),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Setting(
                i18n.roomEndToEndEncryption(),
                i18n.roomEndToEndEncryptionDescription(),
                isEncrypting,
                canEnableEncryption
            ) { roomSettingsSecurityViewModel.openEnableEncryptionWarning() }
        }
        if (leaveEnableEncryptionWarningOpen) {
            RoomSettingsEnableEncryptionWarning(roomSettingsSecurityViewModel)
        }
    }
}


@Composable
fun RoomSettingsEnableEncryptionWarning(roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel) {
    val i18n = DI.get<I18nView>()
    val isChat = roomSettingsSecurityViewModel.isChat.collectAsState().value
    val encryptionWarningTitle = if (isChat) {
        i18n.roomSettingsEnableEncryptionWarningTitleChat()
    } else {
        i18n.roomSettingsEnableEncryptionWarningTitleGroup()
    }
    val encryptionWarningMessage = if (isChat) {
        i18n.roomSettingsEnableEncryptionWarningMessageChat()
    } else {
        i18n.roomSettingsEnableEncryptionWarningMessageGroup()
    }

    WarningDialog(
        title = encryptionWarningTitle,
        message = { Text(encryptionWarningMessage) },
        dismissButtonText = i18n.commonCancel().capitalize(Locale.current),
        confirmButtonText = i18n.roomEnableEncryptionWarningConfirmation(),
        dismissAction = {
            roomSettingsSecurityViewModel.closeEnableEncryptionWarning()
        },
        confirmAction = {
            roomSettingsSecurityViewModel.enableEncryption()
            roomSettingsSecurityViewModel.closeEnableEncryptionWarning()
        }
    )
}
