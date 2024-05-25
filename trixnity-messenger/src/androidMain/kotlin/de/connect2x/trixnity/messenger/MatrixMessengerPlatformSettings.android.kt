package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatrixMessengerAccountPlatformNotificationSettings(
    val pushMode: PushMode = PushMode.PUSH
) : SettingsView<MatrixMessengerAccountPlatformSettings>

val MatrixMessengerAccountPlatformSettings.notifications
        by settingsView<MatrixMessengerAccountPlatformSettings, MatrixMessengerAccountPlatformNotificationSettings>()

@Serializable
enum class PushMode {
    @SerialName("polling")
    POLLING,

    @SerialName("push")
    PUSH,
}
