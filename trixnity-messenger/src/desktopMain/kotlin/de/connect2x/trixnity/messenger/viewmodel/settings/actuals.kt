package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.util.I18n
import kotlinx.coroutines.runBlocking
import org.koin.core.Koin

actual fun doConfigureNotifications(accountName: String, onShowConfigureNotifications: () -> Unit) {
    onShowConfigureNotifications()
}

actual fun getNotificationSettings(accountName: String, koin: Koin): String {
    val i18n = koin.get<I18n>()
    val messengerSettings = koin.get<MessengerSettings>()
    return runBlocking {
        val sound =
            if (messengerSettings.notificationPlaySound[accountName] != false)
                i18n.settingsNotificationsSound()
            else
                i18n.settingsNotificationsSilent()
        val bubble =
            if (messengerSettings.notificationsShowPopup[accountName] != false)
                i18n.settingsNotificationsPopup()
            else
                i18n.settingsNotificationsPopupNot()
        val text =
            if (messengerSettings.notificationsShowText[accountName] != false)
                i18n.settingsNotificationsText()
            else
                i18n.settingsNotificationsTextNot()

        listOf(sound, bubble, text).joinToString(", ")
    }
}