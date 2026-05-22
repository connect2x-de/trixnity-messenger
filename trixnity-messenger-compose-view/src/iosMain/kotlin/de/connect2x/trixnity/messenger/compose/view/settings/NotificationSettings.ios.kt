package de.connect2x.trixnity.messenger.compose.view.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.compose.view.DI
import de.connect2x.trixnity.messenger.compose.view.get
import de.connect2x.trixnity.messenger.compose.view.i18n.I18nView
import de.connect2x.trixnity.messenger.compose.view.theme.components
import de.connect2x.trixnity.messenger.compose.view.theme.components.ThemedButton
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

private val log: Logger = Logger("de.connect2x.trixnity.messenger.compose.view.settings.NotificationSettingsKt")

@Composable
internal actual fun ColumnScope.PlatformDeviceNotificationSettings(
    viewModel: NotificationSettingsSingleAccountViewModel
) {
    val i18n = DI.get<I18nView>()
    val enabled = viewModel.enabledForThisDevice.collectAsState().value
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
        },
    ) {
        Text(i18n.notificationsSettingsPlatform())
    }
}
