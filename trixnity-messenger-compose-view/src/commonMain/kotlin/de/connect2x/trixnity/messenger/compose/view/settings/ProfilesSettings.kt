package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.RunningText
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModel

interface ProfilesSettingsView {
    @Composable fun create(profilesSettingsViewModel: ProfilesSettingsViewModel)
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
        val profilesDialogueController = ProfilesDialogueController()

        Box(Modifier.fillMaxSize()) {
            Column {
                Header(profilesSettingsViewModel::close, i18n.profilesSettings())
                Box {
                    Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                        MultiProfileSettingsCard(profilesSettingsViewModel)
                        ProfileListSettingsCard(profilesSettingsViewModel, profilesDialogueController)
                    }
                    VerticalScrollbar(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), scroll)
                }
            }
        }
    }

    @Composable
    private fun MultiProfileSettingsCard(profilesSettingsViewModel: ProfilesSettingsViewModel) {
        val i18n = DI.get<I18nView>()
        val multiProfileEnabled = profilesSettingsViewModel.isMultiProfile.collectAsState().value
        val canChangeMultiProfileMode = profilesSettingsViewModel.canChangeMultiProfileMode.collectAsState().value

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
                onChange = { profilesSettingsViewModel.setMultiProfileEnabled(it) },
            )
        }
    }

    @Composable
    private fun ProfileListSettingsCard(
        profilesSettingsViewModel: ProfilesSettingsViewModel,
        profilesDialogueController: ProfilesDialogueController,
    ) {
        val i18n = DI.get<I18nView>()
        val multiProfileEnabled = profilesSettingsViewModel.isMultiProfile.collectAsState().value

        SettingsCard(i18n.profilesSettingsList()) {
            ProfileList(profilesSettingsViewModel, profilesDialogueController)
            SmallSpacer()
            if (multiProfileEnabled) {
                ThemedButton(
                    style = MaterialTheme.components.primaryButton,
                    onClick = {
                        profilesDialogueController.openCreateDialogue(profilesSettingsViewModel.activeProfile.value)
                    },
                ) {
                    Text(i18n.selectProfileCreateInstead())
                }
            }
        }
    }
}
