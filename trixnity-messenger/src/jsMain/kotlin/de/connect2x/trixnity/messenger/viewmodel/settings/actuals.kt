package de.connect2x.trixnity.messenger.viewmodel.settings

import org.koin.core.Koin

actual fun doConfigureNotifications(accountName: String, onShowConfigureNotifications: () -> Unit) {
    onShowConfigureNotifications()
}

actual fun getNotificationSettings(accountName: String, koin: Koin): String {
    return ""
}