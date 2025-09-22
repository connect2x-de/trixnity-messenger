package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
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
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.VerticalScrollbar
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.MoreOptions
import de.connect2x.messenger.compose.view.common.RadioSetting
import de.connect2x.messenger.compose.view.common.RadioSettingOption
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedListItemSwitch
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettings
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
        val notificationSettings = notificationsSettingsViewModel.notificationSettings.collectAsState().value
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
    val i18n = DI.get<I18nView>()

    val enabledForThisDevice by viewModel.enabledForThisDevice.collectAsState()
    SettingsAccountCard(viewModel.account) {
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsEnabledForThisDevice()) },
            selected = enabledForThisDevice,
            onChange = { viewModel.toggleEnabledForThisDevice() },
        )
        Spacer(Modifier.size(16.dp))
        PlatformNotificationSettings(viewModel, enabledForThisDevice)
        Spacer(Modifier.size(16.dp))
        PlatformNotificationAccountSettings(viewModel, enabledForThisDevice)
    }
}

@Composable
internal expect fun ColumnScope.PlatformNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel,
    enabled: Boolean = true
)

@Composable
fun ColumnScope.PlatformNotificationAccountSettings(
    viewModel: NotificationSettingsSingleAccountViewModel,
    enabled: Boolean = true
) {
    val i18n = DI.get<I18nView>()
    val settings by viewModel.accountSettings.collectAsState()
    val isUpdating by viewModel.accountSettingsIsUpdating.collectAsState()
    val canChangeSettings = !isUpdating && enabled

    RadioSetting(
        text = i18n.notificationsSettingsAccountDefaultLevel(
            when (settings.defaultLevel) {
                NotificationSettings.DefaultLevel.ROOM -> i18n.notificationsSettingsAccountDefaultLevelRoom()
                NotificationSettings.DefaultLevel.DM -> i18n.notificationsSettingsAccountDefaultLevelDM()
                NotificationSettings.DefaultLevel.MENTION -> i18n.notificationsSettingsAccountDefaultLevelMention()
                else -> i18n.notificationsSettingsAccountDefaultLevelNone()
            }
        ),
        icon = Icons.Filled.AlternateEmail,
        options = mapOf(
            NotificationSettings.DefaultLevel.ROOM to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelRoom(),
                enabled = canChangeSettings,
                style = MaterialTheme.typography.labelLarge
            ),
            NotificationSettings.DefaultLevel.DM to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelDM(),
                enabled = canChangeSettings,
                style = MaterialTheme.typography.labelLarge
            ),
            NotificationSettings.DefaultLevel.MENTION to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelMention(),
                enabled = canChangeSettings,
                style = MaterialTheme.typography.labelLarge
            ),
            NotificationSettings.DefaultLevel.NONE to RadioSettingOption(
                text = i18n.notificationsSettingsAccountDefaultLevelNone(),
                enabled = canChangeSettings,
                style = MaterialTheme.typography.labelLarge
            ),
        ),
        value = settings.defaultLevel,
        set = { viewModel.updateAccountSettings(settings.copy(defaultLevel = it)) },
    )

    MiddleSpacer()

    MoreOptions(
        title = { Text(i18n.notificationsSettingsAccountSound(), style = MaterialTheme.typography.titleSmall) },
        icon = Icons.Filled.NotificationsActive,
    ) {
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundRoom()) },
            selected = settings.sound.room,
            enabled = canChangeSettings && settings.defaultLevel >= NotificationSettings.DefaultLevel.ROOM,
            onChange = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(room = !settings.sound.room))) },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundDM()) },
            selected = settings.sound.dm,
            enabled = canChangeSettings && settings.defaultLevel >= NotificationSettings.DefaultLevel.DM,
            onChange = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(dm = !settings.sound.dm))) },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundMention()) },
            selected = settings.sound.mention,
            enabled = canChangeSettings && settings.defaultLevel >= NotificationSettings.DefaultLevel.MENTION,
            onChange = {
                viewModel.updateAccountSettings(
                    settings.copy(
                        sound = settings.sound.copy(
                            mention = !settings.sound.mention
                        )
                    )
                )
            },
        )
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountSoundCall()) },
            selected = settings.sound.call,
            enabled = canChangeSettings && settings.defaultLevel > NotificationSettings.DefaultLevel.NONE,
            onChange = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(call = !settings.sound.call))) },
        )
    }

    MiddleSpacer()
    MoreOptions(
        title = { Text(i18n.notificationsSettingsAccountOthers(), style = MaterialTheme.typography.titleSmall) },
        icon = Icons.Filled.Notifications,
    ) {
        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountActivityInvite()) },
            selected = settings.activity.invite,
            enabled = canChangeSettings && settings.defaultLevel > NotificationSettings.DefaultLevel.NONE,
            onChange = {
                viewModel.updateAccountSettings(
                    settings.copy(
                        activity = settings.activity.copy(
                            invite = !settings.activity.invite
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountActivityStatus()) },
            selected = settings.activity.status,
            enabled = canChangeSettings && settings.defaultLevel > NotificationSettings.DefaultLevel.NONE,
            onChange = {
                viewModel.updateAccountSettings(
                    settings.copy(
                        activity = settings.activity.copy(
                            status = !settings.activity.status
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountActivityNotice()) },
            selected = settings.activity.notice,
            enabled = canChangeSettings && settings.defaultLevel > NotificationSettings.DefaultLevel.NONE,
            onChange = {
                viewModel.updateAccountSettings(
                    settings.copy(
                        activity = settings.activity.copy(
                            notice = !settings.activity.notice
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountMentionUser(viewModel.account)) },
            selected = settings.mention.user,
            enabled = canChangeSettings && settings.defaultLevel >= NotificationSettings.DefaultLevel.MENTION,
            onChange = {
                viewModel.updateAccountSettings(
                    settings.copy(
                        mention = settings.mention.copy(
                            user = !settings.mention.user
                        )
                    )
                )
            },
        )

        ThemedListItemSwitch(
            style = MaterialTheme.components.settingsItem,
            headlineContent = { Text(i18n.notificationsSettingsAccountMentionRoom()) },
            selected = settings.mention.room,
            enabled = canChangeSettings && settings.defaultLevel >= NotificationSettings.DefaultLevel.MENTION,
            onChange = {
                viewModel.updateAccountSettings(
                    settings.copy(
                        mention = settings.mention.copy(
                            room = !settings.mention.room
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

    val updateError = viewModel.accountSettingsUpdateError.collectAsState().value
    if (updateError != null) {
        MiddleSpacer()
        Text(i18n.anErrorHasOccurred())
        SmallSpacer()
        Text(updateError, color = MaterialTheme.colorScheme.error)
    }
}
