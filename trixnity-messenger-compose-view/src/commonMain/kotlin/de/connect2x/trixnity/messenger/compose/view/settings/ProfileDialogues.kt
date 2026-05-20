package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.collectAsTextFieldValueState
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.profiles.ProfileCreation
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsSingleViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModel

@Composable
fun ProfileDialogues(
    profilesSettingsViewModel: ProfilesSettingsViewModel,
    profilesSettingsSingleViewModel: ProfilesSettingsSingleViewModel,
    profilesDialogueController: ProfilesDialogueController,
) {

    val openedDialogueType = profilesDialogueController.openedDialogueType.value
    val profiles = profilesSettingsViewModel.profiles.collectAsState().value
    val profileName = profilesSettingsSingleViewModel.profileName.value

    when (openedDialogueType) {
        ProfileDialogue.RENAME ->
            RenameProfileDialogue(
                onConfirm = {
                    profilesSettingsSingleViewModel.changeProfileName()
                    profilesDialogueController.closeOpenedDialogue()
                },
                onCancel = { profilesDialogueController.closeOpenedDialogue() },
                profileNameTextField = profilesSettingsSingleViewModel.profileNameTextField,
                profileName = profileName,
                error = profilesSettingsSingleViewModel.profileNameError.collectAsState().value,
            )

        ProfileDialogue.SELECT -> {
            val activeProfile = profilesSettingsViewModel.activeProfile.collectAsState().value
            val activeProfileName = profiles[activeProfile]?.profileName?.collectAsState()?.value ?: ""
            SelectProfileDialogue(
                onConfirm = {
                    profilesSettingsSingleViewModel.selectProfile()
                    profilesDialogueController.closeOpenedDialogue()
                },
                onCancel = { profilesDialogueController.closeOpenedDialogue() },
                profileName = profileName,
            )
        }

        ProfileDialogue.DELETE ->
            DeleteProfileDialogue(
                onConfirm = {
                    profilesSettingsSingleViewModel.deleteProfile()
                    profilesDialogueController.closeOpenedDialogue()
                },
                onCancel = { profilesDialogueController.closeOpenedDialogue() },
                profileName = profileName,
            )

        ProfileDialogue.CREATE ->
            ProfileCreation(
                textFieldViewModel = profilesSettingsViewModel.profileCreationTextField,
                error = profilesSettingsViewModel.profileCreationError.collectAsState().value,
                onFinish = { profilesDialogueController.closeOpenedDialogue() },
                onCreate = { profilesSettingsViewModel.createProfile() },
            )

        null -> {}
    }
}

@Composable
fun RenameProfileDialogue(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    profileNameTextField: TextFieldViewModel,
    profileName: String,
    error: String?,
) {
    val i18n = DI.get<I18nView>()
    var newName by profileNameTextField.collectAsTextFieldValueState()

    ThemedModalDialog(onCancel) {
        ModalDialogHeader { Text(i18n.profileRenameDialogueHeader()) }
        ModalDialogContent {
            Text(i18n.profileRenameDialogueBody(profileName), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = newName, onValueChange = { newName = it }, isError = error != null, maxLines = 1)
            if (error != null) {
                Spacer(Modifier.size(5.dp))
                Text(color = MaterialTheme.colorScheme.error, text = error)
            }
        }
        DialogueFooter(
            onConfirm = { onConfirm() },
            onCancel = onCancel,
            confirmText = i18n.profileRenameDialogueConfirm(),
            enableConfirm = (error == null),
        )
    }
}

@Composable
fun SelectProfileDialogue(onConfirm: () -> Unit, onCancel: () -> Unit, profileName: String) {
    val i18n = DI.get<I18nView>()
    ThemedModalDialog(onCancel) {
        ModalDialogHeader { Text(i18n.profileSelectDialogueHeader(profileName)) }
        DialogueFooter(onConfirm, onCancel, i18n.commonConfirm())
    }
}

@Composable
fun DeleteProfileDialogue(onConfirm: () -> Unit, onCancel: () -> Unit, profileName: String) {
    val i18n = DI.get<I18nView>()
    ThemedModalDialog(onCancel) {
        ModalDialogHeader { Text(i18n.profileDeleteDialogueHeader(profileName)) }
        ModalDialogContent { Text(i18n.profileDeleteDialogueBody(), style = MaterialTheme.typography.titleMedium) }
        DialogueFooter(onConfirm, onCancel, i18n.profileDeleteDialogueConfirm())
    }
}

@Composable
private fun DialogueFooter(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmText: String,
    enableConfirm: Boolean = true,
) {
    val i18n = DI.get<I18nView>()
    ModalDialogFooter {
        ThemedButton(style = MaterialTheme.components.commonButton, onClick = onCancel) { Text(i18n.actionCancel()) }
        ThemedButton(style = MaterialTheme.components.primaryButton, enabled = enableConfirm, onClick = onConfirm) {
            Text(confirmText)
        }
    }
}

class ProfilesDialogueController {
    var openedDialogueType: MutableState<ProfileDialogue?> = mutableStateOf(null)
    var openedDialogueProfileId: MutableState<String?> = mutableStateOf(null)

    fun openRenameDialogue(profileId: String) {
        openedDialogueType.value = ProfileDialogue.RENAME
        openedDialogueProfileId.value = profileId
    }

    fun openSelectDialogue(profileId: String) {
        openedDialogueType.value = ProfileDialogue.SELECT
        openedDialogueProfileId.value = profileId
    }

    fun openDeleteDialogue(profileId: String) {
        openedDialogueType.value = ProfileDialogue.DELETE
        openedDialogueProfileId.value = profileId
    }

    // The profile Id is not acctually used inside the create Profile Dialogue, but makes the code simpler
    fun openCreateDialogue(profileId: String?) {
        openedDialogueType.value = ProfileDialogue.CREATE
        openedDialogueProfileId.value = profileId
    }

    fun closeOpenedDialogue() {
        openedDialogueType.value = null
        openedDialogueProfileId.value = null
    }
}

enum class ProfileDialogue {
    RENAME,
    DELETE,
    SELECT,
    CREATE,
}
