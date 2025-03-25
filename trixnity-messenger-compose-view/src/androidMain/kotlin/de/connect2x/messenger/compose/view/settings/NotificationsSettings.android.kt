package de.connect2x.messenger.compose.view.settings

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel
import net.folivo.trixnity.core.model.UserId

private fun pushChannelId(userId: UserId, config: MatrixMessengerConfiguration) = "${config.appId}.push.${
    userId.full.replace("[@.]".toRegex(), "_")
}"

@Composable
fun DeviceSettingsButton(viewModel: NotificationSettingsSingleAccountViewModel, enabled: Boolean) {
    val context = LocalContext.current
    val messengerConfig = DI.get<MatrixMessengerConfiguration>()
    val i18n = DI.get<I18nView>()
    ThemedButton(
        style = MaterialTheme.components.primaryButton,
        onClick = {
            val intent: Intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(
                android.provider.Settings.EXTRA_APP_PACKAGE,
                context.packageName
            ).putExtra(
                android.provider.Settings.EXTRA_CHANNEL_ID, pushChannelId(viewModel.account, messengerConfig)
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        },
        enabled = enabled
    ) {
        Text(i18n.notificationsSettingsPlatform())
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
internal actual fun ColumnScope.PlatformNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel,
    enabled: Boolean
) {
    val i18n = DI.get<I18nView>()
    val permissionNecessary = viewModel.notificationPermissionsNecessary.collectAsState().value

    if (permissionNecessary) {
        Row {
            Icon(Icons.Default.Error, i18n.notificationSettingsPlatformEnablePermissionsWarning())
            SmallSpacer()
            Text(i18n.notificationSettingsPlatformEnablePermissionsWarning())
        }
        SmallSpacer()
    }
    DeviceSettingsButton(viewModel, enabled)
}
