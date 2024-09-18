package de.connect2x.messenger.compose.view.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel
import net.folivo.trixnity.core.model.UserId

private fun pushChannelId(userId: UserId, config: MatrixMessengerConfiguration) = "${config.packageName}.push.${
    userId.full.replace("[@.]".toRegex(), "_")
}"

@Composable
fun DeviceSettingsButton(viewModel: NotificationSettingsSingleAccountViewModel) {
    val context = DI.get<Context>()
    val messengerConfig = DI.get<MatrixMessengerConfiguration>()
    val i18n = DI.get<I18nView>()
    Button(onClick = {
        val intent: Intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(
            android.provider.Settings.EXTRA_APP_PACKAGE,
            context.packageName
        ).putExtra(
            android.provider.Settings.EXTRA_CHANNEL_ID, pushChannelId(viewModel.account, messengerConfig)
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }) {
        Text(i18n.notificationsSettingsPlatform())
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
internal actual fun ColumnScope.PlatformNotificationSettings(viewModel: NotificationSettingsSingleAccountViewModel) {
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
    DeviceSettingsButton(viewModel)
}
