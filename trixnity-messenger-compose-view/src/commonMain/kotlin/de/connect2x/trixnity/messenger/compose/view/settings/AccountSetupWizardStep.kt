package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.RadioSetting
import de.connect2x.trixnity.messenger.compose.view.common.RadioSettingOption
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.common.wizard.WizardSection
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.compose.view.theme.messengerDpConstants
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountSetupViewModel

@Composable
fun AccountSetupWizardStep(viewModel: AccountSetupViewModel) {
    val i18n = DI.get<I18nView>()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(i18n.initialSettingsSetupWelcomeMessage())
        SetupStrictPrivacySettings(viewModel)
        SetupNotificationSettings(viewModel)
    }
}

@Composable
fun SetupStrictPrivacySettings(viewModel: AccountSetupViewModel) {
    val i18n = DI.get<I18nView>()

    val strictPrivacyEnabled by viewModel.initialSettingsSetupViewModel.strictPrivacyEnabled.collectAsState()

    WizardSection(contentPadding = PaddingValues.Zero) {
        ThemedListItemSwitch(
            style =
                MaterialTheme.components.settingsItem.copy(
                    contentPadding = PaddingValues(MaterialTheme.messengerDpConstants.middle)
                ),
            headlineContent = {
                Text(i18n.initialSettingsSetupStrictPrivacy(), style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Text(i18n.initialSettingsSetupStrictPrivacyExplanation(), style = MaterialTheme.typography.bodyMedium)
            },
            selected = strictPrivacyEnabled,
            onChange = { viewModel.initialSettingsSetupViewModel.toggleStrictPrivacy() },
        )
    }
}

@Composable
fun SetupNotificationSettings(viewModel: AccountSetupViewModel) {
    val i18n = DI.get<I18nView>()

    val initialSettingsSetupViewModel = viewModel.initialSettingsSetupViewModel
    val notificationsEnabled by initialSettingsSetupViewModel.notificationsEnabled.collectAsState()
    val availableProviders = initialSettingsSetupViewModel.availableProviders
    val selectedProvider = initialSettingsSetupViewModel.selectedProvider.collectAsState().value
    val permissionNecessary = initialSettingsSetupViewModel.notificationPermissionsNecessary.collectAsState().value

    WizardSection(contentPadding = PaddingValues.Zero) {
        ThemedListItemSwitch(
            style =
                MaterialTheme.components.settingsItem.copy(
                    contentPadding = PaddingValues(MaterialTheme.messengerDpConstants.middle)
                ),
            headlineContent = {
                Text(
                    i18n.commonNotifications().capitalize(Locale.current),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            supportingContent = {
                Text(i18n.initialSettingsSetupNotificationsExplanation(), style = MaterialTheme.typography.bodyMedium)
            },
            selected = notificationsEnabled,
            onChange = { initialSettingsSetupViewModel.toggleNotifications() },
        )
        if (permissionNecessary) {
            Row(Modifier.padding(horizontal = MaterialTheme.messengerDpConstants.middle)) {
                Icon(Icons.Default.Error, i18n.notificationSettingsPlatformEnablePermissionsWarning())
                SmallSpacer()
                Text(i18n.notificationSettingsPlatformEnablePermissionsWarning())
            }
        }
        PlatformDeviceNotificationSettings(
            modifier =
                Modifier.padding(
                    start = MaterialTheme.messengerDpConstants.middle,
                    end = MaterialTheme.messengerDpConstants.middle,
                    bottom = MaterialTheme.messengerDpConstants.middle,
                ),
            enabled = initialSettingsSetupViewModel.notificationsEnabled.collectAsState().value,
            notificationHandlerId = initialSettingsSetupViewModel.notificationHandlerId,
        )

        if (availableProviders.size > 1 && selectedProvider != null) {
            Column(Modifier.padding(horizontal = MaterialTheme.messengerDpConstants.middle)) {
                RadioSetting(
                    text = i18n.notificationsSettingsProvider(),
                    icon = Icons.Default.CloudDownload,
                    options =
                        availableProviders.associate { provider ->
                            provider.id to RadioSettingOption(text = provider.displayName)
                        },
                    value = selectedProvider.id,
                    set = { initialSettingsSetupViewModel.selectProvider(it) },
                    enabled = notificationsEnabled,
                )
            }
        }
        if (permissionNecessary || availableProviders.size > 1 && selectedProvider != null) {
            MiddleSpacer()
        }
    }
}
