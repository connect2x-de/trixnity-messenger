package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import kotlinx.serialization.Serializable

@Serializable
data class MatrixMessengerAccountPlatformSettingsAndroid(
    val notificationsPlaySound: Boolean = true,
    val notificationsShowPopup: Boolean = true,
    val notificationsShowText: Boolean = true,
) : SettingsView<MatrixMessengerAccountPlatformSettings>

val MatrixMessengerAccountPlatformSettings.notifications
        by settingsView<MatrixMessengerAccountPlatformSettings, MatrixMessengerAccountPlatformSettingsAndroid>()
