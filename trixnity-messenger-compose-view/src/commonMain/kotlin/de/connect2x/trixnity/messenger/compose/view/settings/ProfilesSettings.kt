package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.RunningText
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.Tooltip
import de.connect2x.trixnity.messenger.compose.view.common.icons.EditIcon
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.profiles.ProfileCreation
import de.connect2x.trixnity.messenger.compose.view.roomlist.header.CloseProfile
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogContent
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogFooter
import de.connect2x.trixnity.messenger.compose.view.theme.components.ModalDialogHeader
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedIconButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedModalDialog
import de.connect2x.trixnity.messenger.multi.ProfileCreationViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfileDialogue
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModel

interface ProfilesSettingsView {
    @Composable
    fun create(profilesSettingsViewModel: ProfilesSettingsViewModel)
}

@Composable
fun ProfilesSettings(profilesSettingsViewModel: ProfilesSettingsViewModel) {
    DI.get<ProfilesSettingsView>().create(profilesSettingsViewModel)
}

class ProfilesSettingsViewImpl : ProfilesSettingsView {
    @Composable
    override fun create(profilesSettingsViewModel: ProfilesSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val scroll = rememberScrollState()
        val multiProfileEnabled = profilesSettingsViewModel.isMultiProfile.collectAsState().value
        val canChangeMultiProfileMode = profilesSettingsViewModel.canChangeMultiProfileMode.collectAsState().value

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(profilesSettingsViewModel::close, i18n.profilesSettings())
                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        SettingsCard(i18n.multiProfilesModeSettings()) {
                            RunningText(i18n.multiProfileModeDescription())
                            ThemedListItemSwitch(
                                headlineContent = { Text(i18n.profileSelectionMultipleAccountSwitch()) },
                                enabled = canChangeMultiProfileMode,
                                selected = multiProfileEnabled,
                                supportingContent = {
                                    if (!canChangeMultiProfileMode) {
                                        Text(i18n.cannotDisableMultiProfileMode())
                                    }
                                },
                                onChange = { profilesSettingsViewModel.setMultiProfileEnabled(it) }
                            )
                        }
                        SettingsCard(i18n.profilesSettingsList()) {
                            ProfilesList(profilesSettingsViewModel)
                            SmallSpacer()
                            if(multiProfileEnabled){
                                ThemedButton(
                                    style = MaterialTheme.components.primaryButton,
                                    onClick = {
                                        profilesSettingsViewModel.openCreateDialogue()
                                    },
                                ) {
                                    Text(i18n.selectProfileCreateInstead())
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        scroll,
                    )
                }
            }
        }
        ProfileDialogues(profilesSettingsViewModel)
    }

    @Composable
    fun ProfilesList(profilesSettingsViewModel: ProfilesSettingsViewModel) {
        val multiProfileEnabled = profilesSettingsViewModel.isMultiProfile.collectAsState().value
        val activeProfile = profilesSettingsViewModel.activeProfile.collectAsState().value
        val profilesSingleViewModels = profilesSettingsViewModel.profilesSingleViewModels.collectAsState().value

        profilesSingleViewModels.forEach {
            val activeProfileIsCurrent = (it.key == activeProfile)
            ThemedListItemButton(
                headlineContent = { Text(it.value.profileName.value) },
                onClick = { profilesSettingsViewModel.openSelectDialogue(it.key) },
                enabled = !activeProfileIsCurrent,
                trailingContent = {
                    if (activeProfileIsCurrent) {
                        Row {
                            RenameProfileButton { profilesSettingsViewModel.openRenameDialogue(it.key) }
                            if(multiProfileEnabled){
                                CloseProfile(true) { profilesSettingsViewModel.closeProfile() }
                            }
                            DeleteProfileButton { profilesSettingsViewModel.openDeleteDialogue(it.key) }
                        }
                    } else {
                        DeleteProfileButton { profilesSettingsViewModel.openDeleteDialogue(it.key) }
                    }
                }
            )
        }
    }

    @Composable
    private fun ProfileDialogues(profilesSettingsViewModel: ProfilesSettingsViewModel){
        val openedDialogueType = profilesSettingsViewModel.openedDialogueType.collectAsState().value
        val openedDialogueProfileId = profilesSettingsViewModel.openedDialogueProfileId.collectAsState().value
        val profilesSingleViewModels = profilesSettingsViewModel.profilesSingleViewModels.collectAsState().value
        val profilesSingleViewModel = profilesSingleViewModels[openedDialogueProfileId]

        val di = DI.current
        val coroutineScope = rememberCoroutineScope()
        val profileCreationViewModel = remember { ProfileCreationViewModelImpl(di, coroutineScope) }

        if(profilesSingleViewModel != null){
            val profileName = profilesSingleViewModel.profileName.collectAsState().value
            fun closeD() {
                profilesSettingsViewModel.closeOpenedDialogue()
            }
            when (openedDialogueType) {
                ProfileDialogue.RENAME -> RenameProfileDialogue(
                    profilesSingleViewModel.profileName.value,
                    { newName -> profilesSingleViewModel.renameProfile(newName); closeD() },
                    { closeD() },
                    profileName)

                ProfileDialogue.SELECT -> {
                    val activeProfile = profilesSettingsViewModel.activeProfile.collectAsState().value
                    val activeProfileName = profilesSingleViewModels[activeProfile]?.profileName?.collectAsState()?.value ?: ""
                    SelectProfileDialogue(
                        { profilesSingleViewModel.selectProfile(); closeD() },
                        { closeD() },
                        profileName,
                        activeProfileName
                    )
                }

                ProfileDialogue.DELETE -> DeleteProfileDialogue(
                    { profilesSingleViewModel.deleteProfile(); closeD() },
                    { closeD() },
                    profileName
                    )

                ProfileDialogue.CREATE -> ProfileCreation(profileCreationViewModel, false){closeD()}

                null -> {}
            }
        }
    }

    @Composable
    fun RenameProfileButton(renameProfile: () -> Unit) {
        val i18n = DI.get<I18nView>()
        Tooltip({ Text(i18n.profileRenameButtonTooltip()) }) {
            ThemedIconButton(
                style = MaterialTheme.components.destructiveIconButton,
                onClick = renameProfile,
            ) {
                EditIcon(
                    Icons.Default.Edit,
                    i18n.profileRenameButtonTooltip(),
                )
            }
        }
    }

    @Composable
    fun RenameProfileDialogue(initialTextFieldValue: String, onConfirm: (String) -> Unit, onCancel: () -> Unit, profileName: String) {
        val i18n = DI.get<I18nView>()
        var newProfileName by remember { mutableStateOf(initialTextFieldValue) }
        ThemedModalDialog(onCancel) {
            ModalDialogHeader {
                Text(i18n.profileRenameDialogueHeader())
            }
            ModalDialogContent {
                Text(i18n.profileRenameDialogueBody(profileName), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    newProfileName,
                    { newProfileName = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                )
            }
            DialogueFooter({ onConfirm(newProfileName) }, onCancel, i18n.profileRenameDialogueConfirm())
        }
    }

    @Composable
    fun SelectProfileDialogue(onConfirm: () -> Unit, onCancel: () -> Unit, profileName: String, activeProfileName: String) {
        val i18n = DI.get<I18nView>()
        ThemedModalDialog(onCancel) {
            ModalDialogHeader {
                Text(i18n.profileSelectDialogueHeader(profileName))
            }
            ModalDialogContent {
                Text(i18n.profileSelectDialogueBody(activeProfileName), style = MaterialTheme.typography.titleMedium)
            }
            DialogueFooter(onConfirm, onCancel, i18n.commonConfirm())
        }
    }

    @Composable
    fun DeleteProfileButton(deleteProfile: () -> Unit) {
        val i18n = DI.get<I18nView>()
        Tooltip({ Text(i18n.profileDeleteButtonTooltip()) }) {
            ThemedIconButton(
                style = MaterialTheme.components.destructiveIconButton,
                onClick = deleteProfile,
            ) {
                EditIcon(
                    Icons.Default.Delete,
                    i18n.profileDeleteButtonTooltip(),
                )
            }
        }
    }

    @Composable
    fun DeleteProfileDialogue(onConfirm: () -> Unit, onCancel: () -> Unit, profileName: String) {
        val i18n = DI.get<I18nView>()
        ThemedModalDialog(onCancel) {
            ModalDialogHeader {
                Text(i18n.profileDeleteDialogueHeader(profileName))
            }
            ModalDialogContent {
                Text(i18n.profileDeleteDialogueBody(), style = MaterialTheme.typography.titleMedium)
            }
            DialogueFooter(onConfirm, onCancel, i18n.profileDeleteDialogueConfirm())
        }
    }

    @Composable
    private fun DialogueFooter(onConfirm: () -> Unit, onCancel: () -> Unit, confirmText: String) {
        val i18n = DI.get<I18nView>()
        ModalDialogFooter {
            ThemedButton(
                style = MaterialTheme.components.commonButton,
                onClick = onCancel,
            ) {
                Text(i18n.actionCancel())
            }
            ThemedButton(
                style = MaterialTheme.components.primaryButton,
                onClick = onConfirm,
            ) {
                Text(confirmText)
            }
        }
    }
}
