package de.connect2x.messenger.compose.view

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.platformNotifications

actual fun shouldShowPopup(currentSettings: MatrixMessengerAccountSettings): Boolean =
    currentSettings.platformNotifications.notificationsShowPopup

actual fun shouldShowText(currentSettings: MatrixMessengerAccountSettings): Boolean =
    currentSettings.platformNotifications.notificationsShowText

actual fun shouldPlaySound(currentSettings: MatrixMessengerAccountSettings): Boolean =
    currentSettings.platformNotifications.notificationsPlaySound
