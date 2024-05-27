package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import kotlinx.serialization.Serializable

@Serializable
@NestedSettingsView("platform")
data class MatrixMessengerAccountPlatformNotificationSettings(
    val notificationsPlaySound: Boolean = true,
    val notificationsShowPopup: Boolean = true,
    val notificationsShowText: Boolean = true,
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.platformNotifications
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountPlatformNotificationSettings>()
