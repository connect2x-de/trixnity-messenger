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
import de.connect2x.messenger.compose.view.common.ErrorText
import de.connect2x.messenger.compose.view.common.Header
import de.connect2x.messenger.compose.view.common.MiddleSpacer
import de.connect2x.messenger.compose.view.common.RadioSetting
import de.connect2x.messenger.compose.view.common.RadioSettingOption
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
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

        Box(Modifier.fillMaxSize()) {
            Box {
                Column {
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
    }
}

@Composable
fun NotificationSettingsSingleAccount(
    viewModel: NotificationSettingsSingleAccountViewModel,
) {
    val i18n = DI.get<I18nView>()

    val enabledForThisDevice by viewModel.enabledForThisDevice.collectAsState()
    SettingsAccountCard(viewModel.account) {
        Setting(
            text = i18n.notificationsSettingsEnabledForThisDevice(),
            value = enabledForThisDevice,
            toggle = { viewModel.toggleEnabledForThisDevice() }
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

    val soundOptions = listOfNotNull(
        if (settings.defaultLevel >= NotificationSettings.DefaultLevel.ROOM)
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountSoundRoom(),
                value = settings.sound.room,
                toggle = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(room = !settings.sound.room))) },
                enabled = canChangeSettings
            )
        else null,
        if (settings.defaultLevel >= NotificationSettings.DefaultLevel.DM)
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountSoundDM(),
                value = settings.sound.dm,
                toggle = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(dm = !settings.sound.dm))) },
                enabled = canChangeSettings
            )
        else null,
        if (settings.defaultLevel >= NotificationSettings.DefaultLevel.MENTION)
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountSoundMention(),
                value = settings.sound.mention,
                toggle = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(mention = !settings.sound.mention))) },
                enabled = canChangeSettings
            )
        else null,
        if (settings.defaultLevel > NotificationSettings.DefaultLevel.NONE)
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountSoundCall(),
                value = settings.sound.call,
                toggle = { viewModel.updateAccountSettings(settings.copy(sound = settings.sound.copy(call = !settings.sound.call))) },
                enabled = canChangeSettings
            ) else null,
    )
    if (soundOptions.isNotEmpty())
        CollapsableOptionSetting(
            text = i18n.notificationsSettingsAccountSound(),
            icon = Icons.Filled.NotificationsActive,
            options = soundOptions,
        )

    MiddleSpacer()

    val activityOptions = if (settings.defaultLevel > NotificationSettings.DefaultLevel.NONE)
        listOf(
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountActivityInvite(),
                value = settings.activity.invite,
                toggle = { viewModel.updateAccountSettings(settings.copy(activity = settings.activity.copy(invite = !settings.activity.invite))) },
                enabled = canChangeSettings
            ),
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountActivityStatus(),
                value = settings.activity.status,
                toggle = { viewModel.updateAccountSettings(settings.copy(activity = settings.activity.copy(status = !settings.activity.status))) },
                enabled = canChangeSettings
            ),
            OptionSettingOption(
                text = i18n.notificationsSettingsAccountActivityNotice(),
                value = settings.activity.notice,
                toggle = { viewModel.updateAccountSettings(settings.copy(activity = settings.activity.copy(notice = !settings.activity.notice))) },
                enabled = canChangeSettings
            ),
        )
    else listOf()

    val mentionOptions =
        if (settings.defaultLevel >= NotificationSettings.DefaultLevel.MENTION)
            listOf(
                OptionSettingOption(
                    text = i18n.notificationsSettingsAccountMentionUser(viewModel.account),
                    value = settings.mention.user,
                    toggle = { viewModel.updateAccountSettings(settings.copy(mention = settings.mention.copy(user = !settings.mention.user))) },
                    enabled = canChangeSettings
                ),
                OptionSettingOption(
                    text = i18n.notificationsSettingsAccountMentionRoom(),
                    value = settings.mention.room,
                    toggle = { viewModel.updateAccountSettings(settings.copy(mention = settings.mention.copy(room = !settings.mention.room))) },
                    enabled = canChangeSettings
                ),
//            OptionSettingOption( // TODO enable as soon as keywords are supported
//                text = i18n.notificationsSettingsAccountMentionKeyword(),
//                value = settings.mention.keyword,
//                toggle = { viewModel.updateAccountSettings(settings.copy(mention = settings.mention.copy(keyword = !settings.mention.keyword))) },
//                enabled = canChangeSettings && settings.keywords.isNotEmpty()
//            ),
            )
        else emptyList()
    val otherOptions = activityOptions + mentionOptions
    if (otherOptions.isNotEmpty())
        CollapsableOptionSetting(
            text = i18n.notificationsSettingsAccountOthers(),
            icon = Icons.Filled.Notifications,
            options = otherOptions,
        )

    // TODO settings.keywords

    val updateError by viewModel.accountSettingsUpdateError.collectAsState()
    if (updateError != null) {
        Spacer(Modifier.size(16.dp))
        ErrorText(i18n.anErrorHasOccurred(), updateError)
    }
}
