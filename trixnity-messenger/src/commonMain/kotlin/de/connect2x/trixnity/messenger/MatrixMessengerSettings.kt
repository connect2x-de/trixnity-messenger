package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.settings.JsonDelegateSerializer
import de.connect2x.trixnity.messenger.settings.MutableSettings
import de.connect2x.trixnity.messenger.settings.MutableSettingsImpl
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.settings.SettingsHolderImpl
import de.connect2x.trixnity.messenger.settings.SettingsImpl
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.get
import de.connect2x.trixnity.messenger.settings.set
import de.connect2x.trixnity.messenger.util.SecretByteArray
import de.connect2x.trixnity.messenger.util.SecretByteArrayKey
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

@Serializable
data class MatrixMessengerSettingsBase(
    val secretByteArrayKey: SecretByteArrayKey? = null,
    val accounts: Map<UserId, MatrixMessengerAccountSettings> = mapOf(),
    val preferredLang: String? = null,
    val selectedAccount: UserId? = null, // TODO should be saved via decompose state preservation
    val ssoState: SSOState? = null,
    val platform: MatrixMessengerPlatformSettings = MatrixMessengerPlatformSettings(emptyMap()),
) : SettingsView<MatrixMessengerSettings>

@Serializable
data class MatrixMessengerAccountSettingsBase(
    val databasePassword: SecretByteArray?,
    val displayName: String? = null,
    val displayColor: Long? = null,
    val presenceIsPublic: Boolean = true,
    val readMarkerIsPublic: Boolean = true,
    val typingIsPublic: Boolean = true,
    val platform: MatrixMessengerAccountPlatformSettings = MatrixMessengerAccountPlatformSettings(emptyMap()),
) : SettingsView<MatrixMessengerAccountSettings> {
    companion object {
        fun withConfigDefaults(
            databasePassword: SecretByteArray?,
            displayColor: Long?,
            config: MatrixMessengerConfiguration
        ) = MatrixMessengerAccountSettingsBase(
            databasePassword = databasePassword,
            displayColor = displayColor,
            presenceIsPublic = config.defaultPresenceIsPublic,
            readMarkerIsPublic = config.defaultReadMarkerIsPublic,
            typingIsPublic = config.defaultTypingIsPublic,
        )
    }
}

class MatrixMessengerSettings(delegate: Map<String, JsonElement>) : SettingsImpl<MatrixMessengerSettings>(delegate) {
    val base by lazy { get<MatrixMessengerSettings, MatrixMessengerSettingsBase>() }
}

@Serializable(MatrixMessengerAccountSettingsSerializer::class)
class MatrixMessengerAccountSettings(
    delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMessengerAccountSettings>(delegate) {
    val base by lazy { get<MatrixMessengerAccountSettings, MatrixMessengerAccountSettingsBase>() }
}

internal object MatrixMessengerAccountSettingsSerializer : JsonDelegateSerializer<MatrixMessengerAccountSettings>(
    "MatrixMessengerAccountSettingsSerializer", ::MatrixMessengerAccountSettings
)

interface MatrixMessengerSettingsHolder : SettingsHolder<MatrixMessengerSettings> {
    operator fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?>

    suspend fun update(
        userId: UserId,
        updater: MutableSettings<MatrixMessengerAccountSettings>.(MatrixMessengerAccountSettings) -> Unit,
    )

    suspend fun delete(userId: UserId)
}

class MatrixMessengerSettingsHolderImpl(
    storage: SettingsStorage,
) : SettingsHolderImpl<MatrixMessengerSettings>(storage, ::MatrixMessengerSettings),
    MatrixMessengerSettingsHolder {
    override operator fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> =
        map { it.base.accounts[userId] }

    override suspend fun update(
        userId: UserId,
        updater: MutableSettings<MatrixMessengerAccountSettings>.(MatrixMessengerAccountSettings) -> Unit,
    ) = update {
        val oldAccounts = it.base.accounts
        val oldAccountSettings = oldAccounts[userId] ?: MatrixMessengerAccountSettings(emptyMap())
        val newAccountSettings = MutableSettingsImpl(oldAccountSettings)
        with(newAccountSettings) {
            updater(oldAccountSettings)
        }
        set(it.base.copy(accounts = oldAccounts + (userId to MatrixMessengerAccountSettings(newAccountSettings))))
    }

    override suspend fun delete(userId: UserId) = update {
        set(it.base.copy(accounts = it.base.accounts - userId))
    }
}

expect fun platformMatrixMessengerSettingsHolderModule(): Module
