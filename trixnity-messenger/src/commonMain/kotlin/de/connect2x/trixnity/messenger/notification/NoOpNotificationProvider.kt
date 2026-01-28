package de.connect2x.trixnity.messenger.notification

import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.settingsView
import de.connect2x.trixnity.messenger.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import de.connect2x.trixnity.core.model.UserId

@Serializable
@NestedSettingsView("notification", "provider", "noop")
data class MatrixMessengerAccountNotificationProviderNoOpSettings(
    val enabled: Boolean = false,
) : SettingsView<MatrixMessengerAccountSettings>

val MatrixMessengerAccountSettings.notificationProviderNoOp
        by settingsView<MatrixMessengerAccountSettings, MatrixMessengerAccountNotificationProviderNoOpSettings>()

/**
 * This is a default registered [NotificationProvider].
 * It exists to ensure, that a platform without any other [NotificationProvider] can still enable and disable notifications.
 */
class NoOpNotificationProvider(
    private val settings: MatrixMessengerSettingsHolder,
    coroutineScope: CoroutineScope,
) : NotificationProvider {
    override val id = "de.connect2x.trixnity.messenger.notification.noop"
    override val displayName: String = "no op"
    override val canBeEnabled: Boolean = true

    private val enabledForAccounts =
        settings.map { it.base.accounts.mapValues { it.value.notificationProviderNoOp.enabled } }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                settings.value.base.accounts.mapValues { it.value.notificationProviderNoOp.enabled }
            )


    override val isEnabled: StateFlow<Boolean> =
        enabledForAccounts.map { it.any { it.value } }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                enabledForAccounts.value.any { it.value }
            )

    override fun isEnabled(userId: UserId): Flow<Boolean> =
        enabledForAccounts.map { it[userId] ?: false }

    override suspend fun enable(userId: UserId) {
        settings.update<MatrixMessengerAccountNotificationProviderNoOpSettings>(userId) {
            it.copy(enabled = true)
        }
    }

    override suspend fun disable(userId: UserId) {
        settings.update<MatrixMessengerAccountNotificationProviderNoOpSettings>(userId) {
            it.copy(enabled = false)
        }
    }
}
