package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@NestedSettingsView("platform")
data class MatrixMessengerAccountPlatformNotificationSettings(
    val pushMode: PushMode = PushMode.PUSH
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.platformNotifications
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountPlatformNotificationSettings>()

@Serializable
enum class PushMode {
    @SerialName("polling")
    POLLING,

    @SerialName("push")
    PUSH,
}
