package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemButton
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModel

interface UserSettingsView {
    @Composable
    fun create(userSettingsViewModel: UserSettingsViewModel, mainViewModel: MainViewModel)
}

@Composable
fun UserSettings(userSettingsViewModel: UserSettingsViewModel, mainViewModel: MainViewModel) {
    DI.get<UserSettingsView>().create(userSettingsViewModel, mainViewModel)
}

class UserSettingsViewImpl : UserSettingsView {
    @Composable
    override fun create(userSettingsViewModel: UserSettingsViewModel, mainViewModel: MainViewModel) {
        val i18n = DI.get<I18nView>()
        Box(Modifier.fillMaxSize()) {
            Column {
                Header(userSettingsViewModel::closeUserSettings, i18n.commonSettings().capitalize(Locale.current))
                Column {
                    ProfileInfo(userSettingsViewModel)
                    AppearanceSettings(userSettingsViewModel)
                    PrivacySettings(userSettingsViewModel)
                    DevicesSettings(userSettingsViewModel)
                    NotificationsSettings(userSettingsViewModel)
                    AccountSetupSettings(userSettingsViewModel)
                }
            }
        }
    }
}

@Composable
fun ProfileInfo(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Default.Person, null) },
        headlineContent = { Text(i18n.profileTitle()) },
        onClick = { userSettingsViewModel.showProfile() },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun AppearanceSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Default.Palette, null) },
        headlineContent = { Text(i18n.appearanceTitle()) },
        onClick = { userSettingsViewModel.showAppearanceSettings() },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun PrivacySettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Default.PrivacyTip, null) },
        headlineContent = { Text(i18n.privacyTitle()) },
        onClick = { userSettingsViewModel.showPrivacySettings() },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun DevicesSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Default.Devices, null) },
        headlineContent = { Text(i18n.devicesTitle()) },
        onClick = { userSettingsViewModel.showDevicesSettings() },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun NotificationsSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    ThemedListItemButton(
        leadingContent = { Icon(Icons.Default.Notifications, null) },
        headlineContent = { Text(i18n.commonNotifications().capitalize(Locale.current)) },
        onClick = { userSettingsViewModel.showNotificationsSettings() },
        modifier = Modifier.heightIn(min = 72.dp),
    )
}

@Composable
fun AccountSetupSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    val account = DI.get<MatrixMessengerSettingsHolder>().value.base.selectedAccount
    if (account != null) {
        ThemedListItemButton(
            leadingContent = { Icon(Icons.Default.SettingsSuggest, null) },
            headlineContent = { Text(i18n.accountSetupWizardReset().capitalize(Locale.current)) },
            onClick = { userSettingsViewModel.showAccountSetup(account) },
            modifier = Modifier.heightIn(min = 72.dp),
        )
    }
}
