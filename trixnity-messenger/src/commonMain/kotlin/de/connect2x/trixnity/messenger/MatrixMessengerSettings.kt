package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.SecretByteArray
import de.connect2x.trixnity.messenger.util.SecretByteArrayKey
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import kotlin.jvm.JvmInline

@Serializable
data class MatrixMessengerSettings(
    val secretByteArrayKey: SecretByteArrayKey? = null,
    val accounts: Map<UserId, MatrixMessengerAccountSettings> = mapOf(),
    val preferredLang: String? = null,
    val selectedAccount: UserId? = null, // TODO should be saved via decompose state preservation
    val ssoState: SSOState? = null,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val isHighContrast: Boolean = false,
    val accentColor: Long? = null
)

@Serializable
data class MatrixMessengerAccountSettings(
    val databasePassword: SecretByteArray?,
    val displayName: String? = null,
    val displayColor: Long? = null,
    val pushMode: PushMode = PushMode.NONE,
    val presenceIsPublic: Boolean = true,
    val readMarkerIsPublic: Boolean = true,
    val typingIsPublic: Boolean = true,
    val notificationsPlaySound: Boolean = true,
    val notificationsShowPopup: Boolean = true,
    val notificationsShowText: Boolean = true,
) {
    companion object {
        fun withConfigDefaults(
            databasePassword: SecretByteArray?,
            displayColor: Long?,
            config: MatrixMessengerConfiguration
        ) = MatrixMessengerAccountSettings(
            databasePassword = databasePassword,
            displayColor = displayColor,
            pushMode = config.defaultPushMode,
            presenceIsPublic = config.defaultPresenceIsPublic,
            readMarkerIsPublic = config.defaultReadMarkerIsPublic,
            typingIsPublic = config.defaultTypingIsPublic,
            notificationsPlaySound = config.defaultNotificationPlaySound,
            notificationsShowPopup = config.defaultNotificationShowPopup,
            notificationsShowText = config.defaultNotificationShowText,
        )
    }
}

interface MatrixMessengerSettingsHolder : SettingsHolder<MatrixMessengerSettings> {
    operator fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?>

    suspend fun update(
        userId: UserId,
        updater: (MatrixMessengerAccountSettings?) -> MatrixMessengerAccountSettings?
    )
}

@JvmInline
value class MatrixMessengerSettingsHolderImpl(private val delegate: SettingsHolder<MatrixMessengerSettings>) :
    SettingsHolder<MatrixMessengerSettings> by delegate, MatrixMessengerSettingsHolder {
    override operator fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> =
        map { it.accounts[userId] }

    override suspend fun update(
        userId: UserId,
        updater: (MatrixMessengerAccountSettings?) -> MatrixMessengerAccountSettings?
    ) = update {
        val oldAccounts = it.accounts
        val newAccount = updater(oldAccounts[userId])
        val newAccounts =
            if (newAccount == null) oldAccounts - userId
            else oldAccounts + (userId to newAccount)
        it.copy(accounts = newAccounts)
    }
}

expect fun platformMatrixMessengerSettingsHolderModule(): Module
