package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.VerticalScrollbar
import de.connect2x.trixnity.messenger.compose.view.common.ExpandableSection
import de.connect2x.trixnity.messenger.compose.view.common.Header
import de.connect2x.trixnity.messenger.compose.view.common.MiddleSpacer
import de.connect2x.trixnity.messenger.compose.view.common.RadioSetting
import de.connect2x.trixnity.messenger.compose.view.common.RadioSettingOption
import de.connect2x.trixnity.messenger.compose.view.common.SmallSpacer
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsAllAccountsViewModel
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel

interface NotificationsSettingsView {
    @Composable
    fun create(notificationsSettingsViewModel: NotificationSettingsAllAccountsViewModel)
}

@Composable
fun NotificationsSettings(notificationsSettingsViewModel: NotificationSettingsAllAccountsViewModel) {
    DI.get<NotificationsSettingsView>().create(notificationsSettingsViewModel)
}

class NotificationsSettingsViewImpl : NotificationsSettingsView {
    @Composable
    override fun create(notificationsSettingsViewModel: NotificationSettingsAllAccountsViewModel) {
        val i18n = DI.get<I18nView>()
        val notificationSettings = notificationsSettingsViewModel.notificationSettings
        val scroll = rememberScrollState()

        Column(Modifier.fillMaxSize()) {
            Header(notificationsSettingsViewModel::back, i18n.commonNotifications().capitalize(Locale.current))

            Box {
                Column(Modifier.padding(10.dp).verticalScroll(scroll)) {
                    notificationSettings.map { notificationSettingsSingleAccount ->
                        NotificationSettingsSingleAccount(notificationSettingsSingleAccount)
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

@Composable
fun NotificationSettingsSingleAccount(
    viewModel: NotificationSettingsSingleAccountViewModel,
) {
    SettingsAccountCard(viewModel.account) {
        DeviceNotificationSettings(viewModel)
        SmallSpacer()
        AccountNotificationSettings(viewModel)
    }
}

@Composable
internal expect fun ColumnScope.PlatformDeviceNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel,
)

@Composable
fun ColumnScope.DeviceNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel
) {
    val i18n = DI.get<I18nView>()
    val deviceSettings = viewModel.deviceSettings.collectAsState().value
    val enabledForThisDevice by viewModel.enabledForThisDevice.collectAsState()
    val availableProviders = viewModel.availableProviders
    val selectedProvider = viewModel.selectedProvider.collectAsState().value

    ThemedListItemSwitch(
        style = MaterialTheme.components.settingsItem,
        headlineContent = { Text(i18n.notificationsSettingsEnabledForThisDevice()) },
        selected = enabledForThisDevice,
        onChange = { viewModel.toggleEnabledForThisDevice() },
    )

    SmallSpacer()
    val permissionNecessary = viewModel.notificationPermissionsNecessary.collectAsState().value
    if (permissionNecessary) {
        if (permissionNecessary) {
            Row {
                Icon(Icons.Default.Error, i18n.notificationSettingsPlatformEnablePermissionsWarning())
                SmallSpacer()
                Text(i18n.notificationSettingsPlatformEnablePermissionsWarning())
            }
            SmallSpacer()
        }
    }
    PlatformDeviceNotificationSettings(viewModel)

    SmallSpacer()
    if (availableProviders.size > 1 && selectedProvider != null) {
        RadioSetting(
            text = i18n.notificationsSettingsProvider(),
            icon = Icons.Default.CloudDownload,
            options = availableProviders.associate { provider ->
                provider.id to RadioSettingOption(text = provider.displayName)
            },
            value = selectedProvider.id,
            set = { viewModel.selectProvider(it) },
            enabled = enabledForThisDevice
        )
    }

    SmallSpacer()
    CollapsableOptionSetting(
        text = i18n.notificationsSettingsPlatform(),
        icon = Icons.Default.Devices,
        options = listOf(
            OptionSettingOption(
                text = i18n.notificationsSettingsPlatformPlaySound(),
                value = deviceSettings.playSound,
                toggle = { viewModel.updateDeviceSettings(deviceSettings.copy(playSound = !deviceSettings.playSound)) },
                enabled = enabledForThisDevice
            ),
            OptionSettingOption(
                text = i18n.notificationsSettingsPlatformShowDetails(),
                value = deviceSettings.showDetails,
                toggle = { viewModel.updateDeviceSettings(deviceSettings.copy(showDetails = !deviceSettings.showDetails)) },
                enabled = enabledForThisDevice
            ),
        ),
    )
}

@Composable
fun ColumnScope.AccountNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel,
) {
    val i18n = DI.get<I18nView>()
    val enabledForThisDevice by viewModel.enabledForThisDevice.collectAsState()
    val accountSettings by viewModel.accountSettings.collectAsState()
    val accountSettingsIsUpdating by viewModel.accountSettingsIsUpdating.collectAsState()
    val canChangeAccountSettings = !accountSettingsIsUpdating && enabledForThisDevice

    RadioSetting(
        text = i18n.notificationsSettingsAccountDefaultLevel(
            when (accountSettings.defaultLevel) {
                AccountNotificationSettings.DefaultLevel.ROOM -> i18n.notificationsSettingsAccountDefaultLevelRoom()
                AccountNotificationSettings.DefaultLevel.DM -> i18n.notificationsSettingsAccountDefaultLevelDM()
                AccountNotificationSettings.DefaultLevel.MENTION -> i18n.notificationsSettingsAccountDefaultLevelMention()
                else -> i18n.notificationsSettingsAccountDefaultLevelNone()
            }
        ),
        icon = Icons.Filled.AlternateEmail,
        options = mapOf(
            AccountNotificationSettings.DefaultLevel.ROOM to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelRoom(),
                style = MaterialTheme.typography.labelLarge
            ),
            AccountNotificationSettings.DefaultLevel.DM to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelDM(),
                style = MaterialTheme.typography.labelLarge
            ),
            AccountNotificationSettings.DefaultLevel.MENTION to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelMention(),
                style = MaterialTheme.typography.labelLarge
            ),
            AccountNotificationSettings.DefaultLevel.NONE to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelNone(),
                style = MaterialTheme.typography.labelLarge
            ),
        ),
        enabled = canChangeAccountSettings,
        value = accountSettings.defaultLevel,
        set = { viewModel.updateAccountSettings(accountSettings.copy(defaultLevel = it)) },
    )

    MiddleSpacer()

    ExpandableSection(
        heading = { Text(i18n.notificationsSettingsAccountSound(), style = MaterialTheme.typography.titleSmall) },
        icon = Icons.Default.NotificationsActive
    ) {
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundRoom()) },
            selected = accountSettings.sound.room,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel >= AccountNotificationSettings.DefaultLevel.ROOM,
            onChange = { viewModel.updateAccountSettings(accountSettings.copy(sound = accountSettings.sound.copy(room = !accountSettings.sound.room))) },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundDM()) },
            selected = accountSettings.sound.dm,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel >= AccountNotificationSettings.DefaultLevel.DM,
            onChange = { viewModel.updateAccountSettings(accountSettings.copy(sound = accountSettings.sound.copy(dm = !accountSettings.sound.dm))) },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundMention()) },
            selected = accountSettings.sound.mention,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel >= AccountNotificationSettings.DefaultLevel.MENTION,
            onChange = {
                viewModel.updateAccountSettings(
                    accountSettings.copy(
                        sound = accountSettings.sound.copy(
                            mention = !accountSettings.sound.mention
                        )
                    )
                )
            },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundCall()) },
            selected = accountSettings.sound.call,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel > AccountNotificationSettings.DefaultLevel.NONE,
            onChange = { viewModel.updateAccountSettings(accountSettings.copy(sound = accountSettings.sound.copy(call = !accountSettings.sound.call))) },
        )
    }

    MiddleSpacer()
    ExpandableSection(
        heading = { Text(i18n.notificationsSettingsAccountOthers(), style = MaterialTheme.typography.titleSmall) },
        icon = Icons.Default.Notifications
    ) {
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountActivityInvite()) },
            selected = accountSettings.activity.invite,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel > AccountNotificationSettings.DefaultLevel.NONE,
            onChange = {
                viewModel.updateAccountSettings(
                    accountSettings.copy(
                        activity = accountSettings.activity.copy(
                            invite = !accountSettings.activity.invite
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountActivityStatus()) },
            selected = accountSettings.activity.status,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel > AccountNotificationSettings.DefaultLevel.NONE,
            onChange = {
                viewModel.updateAccountSettings(
                    accountSettings.copy(
                        activity = accountSettings.activity.copy(
                            status = !accountSettings.activity.status
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountActivityNotice()) },
            selected = accountSettings.activity.notice,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel > AccountNotificationSettings.DefaultLevel.NONE,
            onChange = {
                viewModel.updateAccountSettings(
                    accountSettings.copy(
                        activity = accountSettings.activity.copy(
                            notice = !accountSettings.activity.notice
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountMentionUser(viewModel.account)) },
            selected = accountSettings.mention.user,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel >= AccountNotificationSettings.DefaultLevel.MENTION,
            onChange = {
                viewModel.updateAccountSettings(
                    accountSettings.copy(
                        mention = accountSettings.mention.copy(
                            user = !accountSettings.mention.user
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountMentionRoom()) },
            selected = accountSettings.mention.room,
            enabled = canChangeAccountSettings && accountSettings.defaultLevel >= AccountNotificationSettings.DefaultLevel.MENTION,
            onChange = {
                viewModel.updateAccountSettings(
                    accountSettings.copy(
                        mention = accountSettings.mention.copy(
                            room = !accountSettings.mention.room
                        )
                    )
                )
            },
        )

        // TODO enable as soon as keywords are supported
        /*
        Setting(
            text = i18n.notificationsSettingsAccountMentionKeyword(),
            value = settings.mention.keyword,
            enabled = canChangeSettings && settings.keywords.isNotEmpty() && settings.defaultLevel >= NotificationSettings.DefaultLevel.MENTION,
            toggle = { viewModel.updateAccountSettings(settings.copy(mention = settings.mention.copy(keyword = !settings.mention.keyword))) }
        )
         */
    }

    // TODO settings.keywords

    val updateError = viewModel.updateAccountSettingsError.collectAsState().value
    if (updateError != null) {
        MiddleSpacer()
        Text(i18n.anErrorHasOccurred())
        SmallSpacer()
        Text(updateError, color = MaterialTheme.colorScheme.error)
    }
}
