package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.sysnotify.withActivationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.platformNotifications

actual fun shouldShowPopup(currentSettings: MatrixMessengerAccountSettings): Boolean =
    currentSettings.platformNotifications.notificationsShowPopup

actual fun shouldShowText(currentSettings: MatrixMessengerAccountSettings): Boolean =
    currentSettings.platformNotifications.notificationsShowText

actual fun shouldPlaySound(currentSettings: MatrixMessengerAccountSettings): Boolean =
    currentSettings.platformNotifications.notificationsPlaySound

actual fun registerActivationHandler(handler: NotificationHandler, activationCallback: (Notification) -> Unit) {
    handler.withActivationHandler(activationCallback)
}
