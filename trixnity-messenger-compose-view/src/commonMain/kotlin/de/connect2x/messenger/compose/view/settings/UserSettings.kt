package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.buttonPointerModifier
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.MainViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.UserSettingsViewModel

// TODO TIM (showContactList)
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
                    SettingsWizardSettings(userSettingsViewModel)
                }
            }
        }
    }
}

@Composable
fun ProfileInfo(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    SettingItem(
        { Icon(Icons.Default.Person, i18n.profileTitle()) },
        i18n.profileTitle(),
        userSettingsViewModel::showProfile,
    )
}

@Composable
fun AppearanceSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    SettingItem(
        { Icon(Icons.Default.Palette, i18n.appearanceTitle()) },
        i18n.appearanceTitle(),
        userSettingsViewModel::showAppearanceSettings,
    )
}

@Composable
fun PrivacySettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    SettingItem(
        { Icon(Icons.Default.PrivacyTip, i18n.privacyTitle()) },
        i18n.privacyTitle(),
        userSettingsViewModel::showPrivacySettings,
    )
}

@Composable
fun DevicesSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    SettingItem(
        { Icon(Icons.Default.Devices, i18n.devicesTitle()) },
        i18n.devicesTitle(),
        userSettingsViewModel::showDevicesSettings,
    )
}

@Composable
fun NotificationsSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    SettingItem(
        { Icon(Icons.Default.Notifications, i18n.commonNotifications()) },
        i18n.commonNotifications().capitalize(Locale.current),
        userSettingsViewModel::showNotificationsSettings,
    )
}

@Composable
fun SettingsWizardSettings(userSettingsViewModel: UserSettingsViewModel) {
    val i18n = DI.get<I18nView>()
    if (userSettingsViewModel.shouldShowSettingsWizardReset.collectAsState().value) {
        SettingItem(
            { Icon(Icons.Default.SettingsSuggest, i18n.settingsWizardReset()) },
            i18n.settingsWizardReset().capitalize(Locale.current),
            userSettingsViewModel::showSettingsWizard
        )
    }
}

@Composable
fun SettingItem(icon: @Composable () -> Unit, text: String, action: () -> Unit) {
    Box(Modifier
        .fillMaxWidth()
        .clickable { action() }
        .buttonPointerModifier()
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.size(20.dp))
            Text(text)
        }
    }
}

