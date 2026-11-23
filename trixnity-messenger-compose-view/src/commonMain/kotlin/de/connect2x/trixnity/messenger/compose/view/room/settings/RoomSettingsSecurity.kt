package de.connect2x.trixnity.messenger.compose.view.room.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import de.connect2x.trixnity.core.model.events.m.room.JoinRulesEventContent.JoinRule
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.icons.HelpIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItem
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.viewmodel.room.settings.RoomSettingsSecurityViewModel

interface RoomSettingsSecurityView {
    @Composable fun create(roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel)
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
        val joinRule = roomSettingsSecurityViewModel.joinRule.collectAsState()
        val leaveEnableEncryptionWarningOpen =
            roomSettingsSecurityViewModel.isEncryptionWarningOpen.collectAsState().value

        val cantToggleReason: String? = cantToggleReason(isEncrypting, joinRule, canEnableEncryption, i18n)

        Column {
            ThemedListItem(
                style = MaterialTheme.components.settingsItem,
                headlineContent = {
                    Text(i18n.security().capitalize(Locale.current), style = MaterialTheme.typography.titleMedium)
                },
            )
            ThemedListItemSwitch(
                style = MaterialTheme.components.settingsItem,
                headlineContent = { Text(i18n.roomEndToEndEncryption()) },
                supportingContent = { Text(i18n.roomEndToEndEncryptionDescription()) },
                leadingContent = {
                    if (cantToggleReason != null) {
                        HelpIcon(cantToggleReason)
                    }
                },
                selected = isEncrypting,
                enabled = canEnableEncryption == true,
                isSelectable = true,
                onChange = { roomSettingsSecurityViewModel.openEnableEncryptionWarning() },
            )
        }
        if (leaveEnableEncryptionWarningOpen) {
            RoomSettingsEnableEncryptionWarning(roomSettingsSecurityViewModel)
        }
    }
}

fun cantToggleReason(
    isEncrypting: Boolean,
    joinRule: State<JoinRule?>,
    canEnableEncryption: Boolean?,
    i18n: I18nView,
): String? {
    if (isEncrypting) {
        return i18n.roomEndToEndEncryptionForbiddenAlreadyEnabled()
    }

    if (joinRule.value is JoinRule.Public) {
        return i18n.roomEndToEndEncryptionForbiddenJoinRulePublic()
    }

    if (canEnableEncryption == false) {
        return i18n.roomEndToEndEncryptionForbiddenOtherwise()
    }

    return null
}

@Composable
fun RoomSettingsEnableEncryptionWarning(roomSettingsSecurityViewModel: RoomSettingsSecurityViewModel) {
    val i18n = DI.get<I18nView>()
    val isChat = roomSettingsSecurityViewModel.isChat.collectAsState().value

    ThemedModalDialog({ roomSettingsSecurityViewModel.closeEnableEncryptionWarning() }) {
        ModalDialogHeader {
            Text(
                if (isChat) i18n.roomSettingsEnableEncryptionWarningTitleChat()
                else i18n.roomSettingsEnableEncryptionWarningTitleGroup()
            )
        }
        ModalDialogContent {
            Text(
                if (isChat) i18n.roomSettingsEnableEncryptionWarningMessageChat()
                else i18n.roomSettingsEnableEncryptionWarningMessageGroup()
            )
        }
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = { roomSettingsSecurityViewModel.closeEnableEncryptionWarning() },
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = {
                    roomSettingsSecurityViewModel.enableEncryption()
                    roomSettingsSecurityViewModel.closeEnableEncryptionWarning()
                },
            ) {
                Text(i18n.roomEnableEncryptionWarningConfirmation())
            }
        }
    }
}
