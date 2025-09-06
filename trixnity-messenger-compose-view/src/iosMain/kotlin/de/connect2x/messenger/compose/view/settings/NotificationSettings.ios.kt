package de.connect2x.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.messenger.compose.view.DI
import de.connect2x.messenger.compose.view.common.SmallSpacer
import de.connect2x.messenger.compose.view.get
import de.connect2x.messenger.compose.view.i18n.I18nView
import de.connect2x.messenger.compose.view.theme.components
import de.connect2x.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

private val log = KotlinLogging.logger { }

@Composable
fun DeviceSettingsButton(enabled: Boolean) {
    val i18n = DI.get<I18nView>()
    ThemedButton(
        style = MaterialTheme.components.primaryButton,
        enabled = enabled,
        onClick = {
            val appSettingsUrl = NSURL(string = UIApplicationOpenSettingsURLString)
            if (UIApplication.sharedApplication().canOpenURL(appSettingsUrl)) {
                UIApplication.sharedApplication().openURL(appSettingsUrl, emptyMap<Any?, Any>()) {}
            } else {
                log.warn { "Couldn't open $appSettingsUrl" }
            }
        }
    ) {
        Text(i18n.notificationsSettingsPlatform())
    }
}

@Composable
internal actual fun ColumnScope.PlatformNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel,
    enabled: Boolean,
) {
    val i18n = DI.get<I18nView>()
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
    DeviceSettingsButton(enabled)
}
