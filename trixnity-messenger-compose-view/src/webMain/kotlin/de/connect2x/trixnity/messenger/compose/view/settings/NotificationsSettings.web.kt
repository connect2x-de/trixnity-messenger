package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel

@Composable
actual fun ColumnScope.PlatformDeviceNotificationSettings(viewModel: NotificationSettingsSingleAccountViewModel) {
    // Used to configure ios and android
}

@Composable
actual fun ColumnScope.PlatformDeviceNotificationSettings(
    enabled: Boolean,
    notificationHandlerId: String,
    modifier: Modifier,
) {
    // Used to configure ios and android
}
