package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.icons.EditIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.CloseProfile
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModel

@Composable
fun ProfileList(
    profilesSettingsViewModel: ProfilesSettingsViewModel,
    profilesDialogueController: ProfilesDialogueController,
) {
    val i18n = DI.get<I18nView>()
    val multiProfileEnabled = profilesSettingsViewModel.isMultiProfile.collectAsState().value
    val activeProfile = profilesSettingsViewModel.activeProfile.collectAsState().value
    val profiles = profilesSettingsViewModel.profiles.collectAsState().value

    profiles.forEach {
        val profileName = it.value.profileName.collectAsState().value

        if (it.key == activeProfile) {
            ThemedListItemButton(
                headlineContent = {
                    Tooltip(profileName) {
                        Text(
                            text = i18n.profilesSettingsListThisProfile() + profileName,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                },
                onClick = { profilesDialogueController.openSelectDialogue(it.key) },
                enabled = false,
                trailingContent = {
                    Row {
                        RenameProfileButton { profilesDialogueController.openRenameDialogue(it.key) }
                        if (multiProfileEnabled) {
                            CloseProfile { profilesSettingsViewModel.closeProfile() }
                        }
                        DeleteProfileButton { profilesDialogueController.openDeleteDialogue(it.key) }
                    }
                },
            )
        } else {
            ThemedListItemButton(
                headlineContent = {
                    Tooltip(profileName) {
                        Text(
                            text = profileName,
                            fontWeight = FontWeight.Normal,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                },
                onClick = { profilesDialogueController.openSelectDialogue(it.key) },
                enabled = true,
                trailingContent = { DeleteProfileButton { profilesDialogueController.openDeleteDialogue(it.key) } },
            )
        }
        if (profilesDialogueController.openedDialogueProfileId.value == it.key) {
            ProfileDialogues(profilesSettingsViewModel, it.value, profilesDialogueController)
        }
    }
}

@Composable
fun RenameProfileButton(renameProfile: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Tooltip({ Text(i18n.profileRenameButtonTooltip()) }) {
        ThemedIconButton(style = MaterialTheme.components.commonIconButton, onClick = renameProfile) {
            EditIcon(Icons.Default.Edit, i18n.profileRenameButtonTooltip())
        }
    }
}

@Composable
fun DeleteProfileButton(deleteProfile: () -> Unit) {
    val i18n = DI.get<I18nView>()
    Tooltip({ Text(i18n.profileDeleteButtonTooltip()) }) {
        ThemedIconButton(style = MaterialTheme.components.destructiveIconButton, onClick = deleteProfile) {
            EditIcon(Icons.Default.Delete, i18n.profileDeleteButtonTooltip())
        }
    }
}
