package de.connect2x.trixnity.messenger.compose.view.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel

@SuppressLint("UnrememberedMutableState")
@Composable
actual fun ColumnScope.PlatformDeviceNotificationSettings(viewModel: NotificationSettingsSingleAccountViewModel) {
    val i18n = DI.get<I18nView>()
    val context = LocalContext.current
    val enabled = viewModel.enabledForThisDevice.collectAsState().value
    ThemedButton(
        style = MaterialTheme.components.primaryButton,
        onClick = {
            val intent: Intent =
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, viewModel.notificationHandlerId)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        },
        enabled = enabled,
    ) {
        Text(i18n.notificationsSettingsPlatform())
    }
}
