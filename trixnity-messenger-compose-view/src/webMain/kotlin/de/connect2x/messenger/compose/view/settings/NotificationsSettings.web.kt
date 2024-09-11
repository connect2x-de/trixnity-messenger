package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel

@Composable
internal actual fun ColumnScope.PlatformNotificationSettings(viewModel: NotificationSettingsSingleAccountViewModel) {
    val i18n = DI.get<I18nView>()

    val playSound by viewModel.playSound.collectAsState()
    val showPopup by viewModel.showPopup.collectAsState()
    val showText by viewModel.showText.collectAsState()

    CollapsableOptionSetting(
        text = i18n.notificationsSettingsPlatform(),
        options = listOf(
            OptionSettingOption(
                text = i18n.notificationsSettingsPlatformPlaySound(),
                value = playSound,
                toggle = { viewModel.togglePlaySound() }
            ),
            OptionSettingOption(
                text = i18n.notificationsSettingsPlatformShowPopup(),
                value = showPopup,
                toggle = { viewModel.toggleShowPopup() }
            ),
            OptionSettingOption(
                text = i18n.notificationsSettingsPlatformShowText(),
                value = showText,
                toggle = { viewModel.toggleShowText() }
            ),
        ),
    )
}
