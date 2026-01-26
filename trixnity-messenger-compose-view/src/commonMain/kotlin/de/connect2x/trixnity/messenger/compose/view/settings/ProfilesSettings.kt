package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.ApprovableTextField
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.RunningText
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.ApprovableTextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.settings.ProfilesSettingsViewModel
import kotlinx.coroutines.flow.flowOf

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
        val isProfileNameSet = profilesSettingsViewModel.isProfileNameSet.collectAsState().value
        var showProfileRenameDialogue by remember { mutableStateOf<Boolean>(false) }


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
                                    supportingContent = {if(!canChangeMultiProfileMode){Text(i18n.cannotDisableMultiProfileMode())} },
                                    onChange = {
                                        if (!isProfileNameSet && it) {
                                            showProfileRenameDialogue = true
                                        } else {
                                            profilesSettingsViewModel.setMultiProfileEnabled(it)
                                        }
                                    }
                                )
                        }
                        SettingsCard(i18n.profileNameSettings()) {
                            ApprovableTextField(
                                profilesSettingsViewModel.profileNameTextFieldViewModel,
                                true,
                                Modifier,
                                i18n.profileNameTextfield(),
                                i18n.profileNamePlaceholder(),
                            )
                        }
                    }
                    VerticalScrollbar(
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        scroll,
                    )
                }
            }
        }
    }
}
