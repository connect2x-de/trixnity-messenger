package de.connect2x.trixnity.messenger.viewmodel.settings

import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import de.connect2x.trixnity.messenger.MessengerConfig
import de.connect2x.trixnity.messenger.findActivity
import de.connect2x.trixnity.messenger.getContext
import de.connect2x.trixnity.messenger.util.I18n
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.Koin


private val log = KotlinLogging.logger { }

fun pushChannelId(accountName: String) =
    "${MessengerConfig.instance.packageName}.${MessengerConfig.instance.appName}.push.${
        accountName.replace("[@.]".toRegex(), "_")
    }"

actual fun doConfigureNotifications(accountName: String, onShowConfigureNotifications: () -> Unit) {
    val intent: Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, getContext().packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, pushChannelId(accountName))
    getContext().findActivity()?.startActivity(intent)
}

actual fun getNotificationSettings(accountName: String, koin: Koin): String {
    val i18n = koin.get<I18n>()
    return NotificationManagerCompat.from(getContext()).getNotificationChannel(pushChannelId(accountName))
        ?.let { notificationChannel ->
            if (notificationChannel.importance < IMPORTANCE_DEFAULT) {
                listOf(
                    i18n.settingsNotificationsSilent(),
                    i18n.settingsNotificationsVibrationNot(),
                    i18n.settingsNotificationsLightsNot(),
                ).joinToString(", ")
            }
            val sound =
                if (notificationChannel.sound != null) i18n.settingsNotificationsSound()
                else i18n.settingsNotificationsSilent()
            val vibrate =
                if (notificationChannel.shouldVibrate()) i18n.settingsNotificationsVibration()
                else i18n.settingsNotificationsVibrationNot()
            val lights =
                if (notificationChannel.shouldShowLights()) i18n.settingsNotificationsLights()
                else i18n.settingsNotificationsLightsNot()
            listOf(sound, vibrate, lights).joinToString(", ")
        } ?: return "".also { log.warn { "cannot get notification channel with Id ${pushChannelId(accountName)}" } }
}