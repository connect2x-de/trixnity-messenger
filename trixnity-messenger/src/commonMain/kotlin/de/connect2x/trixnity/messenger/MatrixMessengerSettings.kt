package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.util.SecretString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module
import kotlin.jvm.JvmInline

@Serializable
data class MatrixMessengerSettings(
    val accounts: Map<UserId, MatrixMessengerAccountSettings> = mapOf(),
    val preferredLang: String? = null,
    val selectedAccount: UserId? = null, // TODO should be saved via decompose state preservation
)

@Serializable
data class MatrixMessengerAccountSettings(
    val userId: UserId,
    val databasePassword: SecretString?,
    val displayName: String? = null,
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
            userId: UserId,
            databasePassword: SecretString?,
            config: MatrixMessengerConfiguration
        ) = MatrixMessengerAccountSettings(
            userId = userId,
            databasePassword = databasePassword,
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

fun interface CreateMatrixMessengerSettingsHolder {
    suspend operator fun invoke(): MatrixMessengerSettingsHolder
}

expect fun platformCreateMatrixMessengerSettingsHolderModule(): Module