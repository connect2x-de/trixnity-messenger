package de.connect2x.messenger.compose.view.notifications

import de.connect2x.sysnotify.Notification
import de.connect2x.sysnotify.NotificationHandler
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings

actual fun shouldShowPopup(currentSettings: MatrixMessengerAccountSettings): Boolean = true
actual fun shouldShowText(currentSettings: MatrixMessengerAccountSettings): Boolean = true
actual fun shouldPlaySound(currentSettings: MatrixMessengerAccountSettings): Boolean = true

actual fun registerActivationHandler(handler: NotificationHandler, activationCallback: (Notification) -> Unit) = Unit
