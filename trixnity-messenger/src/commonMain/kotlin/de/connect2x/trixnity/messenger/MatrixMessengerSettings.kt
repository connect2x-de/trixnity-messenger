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
import de.connect2x.trixnity.messenger.settings.update
import de.connect2x.trixnity.messenger.util.SecretByteArray
import de.connect2x.trixnity.messenger.util.SecretByteArrayKey
import de.connect2x.trixnity.messenger.viewmodel.connecting.SSOState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import net.folivo.trixnity.core.model.UserId
import org.koin.core.module.Module

private val log = KotlinLogging.logger {  }

@Serializable
data class MatrixMessengerSettingsBase(
    val secretByteArrayKey: SecretByteArrayKey? = null,
    val accounts: Map<UserId, MatrixMessengerAccountSettings> = mapOf(),
    val preferredLang: String? = null,
    val selectedAccount: UserId? = null, // TODO should be saved via decompose state preservation
    val ssoState: SSOState? = null,

    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val isHighContrast: Boolean = false,
    val accentColor: Long? = null
) : SettingsView<MatrixMessengerSettings>

@Serializable
data class MatrixMessengerAccountSettingsBase(
    val databasePassword: SecretByteArray? = null,
    val displayName: String? = null,
    val displayColor: Long? = null,
    val notificationsEnabled: Boolean = true,
    val presenceIsPublic: Boolean = true,
    val readMarkerIsPublic: Boolean = true,
    val typingIsPublic: Boolean = true,
    val accountSetupFinished : Boolean = false
) : SettingsView<MatrixMessengerAccountSettings> {
    companion object {
        fun withConfigDefaults(
            databasePassword: SecretByteArray?,
            displayColor: Long?,
            config: MatrixMessengerConfiguration,
        ) = MatrixMessengerAccountSettingsBase(
            databasePassword = databasePassword,
            displayColor = displayColor,
            notificationsEnabled = config.notificationsEnabled,
            presenceIsPublic = config.defaultPresenceIsPublic,
            readMarkerIsPublic = config.defaultReadMarkerIsPublic,
            typingIsPublic = config.defaultTypingIsPublic,
            accountSetupFinished = config.useAccountSetupWizard.not(),
        )
    }
}

data class MatrixMessengerSettings(
    private val delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMessengerSettings>(delegate) {
    val base by lazy { get<MatrixMessengerSettings, MatrixMessengerSettingsBase>() }
}

@Serializable(MatrixMessengerAccountSettingsSerializer::class)
data class MatrixMessengerAccountSettings(
    private val delegate: Map<String, JsonElement>
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
    settings: MutableStateFlow<MatrixMessengerSettings?> = MutableStateFlow(null)
) : SettingsHolderImpl<MatrixMessengerSettings>(storage, ::MatrixMessengerSettings, settings),
    MatrixMessengerSettingsHolder {
    override operator fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?> =
        map { it.base.accounts[userId] }

    override suspend fun update(
        userId: UserId,
        updater: MutableSettings<MatrixMessengerAccountSettings>.(MatrixMessengerAccountSettings) -> Unit,
    ) = update<MatrixMessengerSettingsBase> {
        log.debug { "update MatrixMessengerSettings" }
        val oldAccounts = it.accounts
        val oldAccountSettings = oldAccounts[userId] ?: MatrixMessengerAccountSettings(emptyMap())
        val newAccountSettings = MutableSettingsImpl(oldAccountSettings)
        with(newAccountSettings) {
            updater(oldAccountSettings)
        }
        it.copy(accounts = oldAccounts + (userId to MatrixMessengerAccountSettings(newAccountSettings)))
    }

    override suspend fun delete(userId: UserId) = update<MatrixMessengerSettingsBase> {
        it.copy(accounts = it.accounts - userId)
    }
}

suspend fun <T : SettingsView<MatrixMessengerAccountSettings>> MatrixMessengerSettingsHolder.update(
    userId: UserId,
    serializer: KSerializer<T>,
    updater: (T) -> T,
) = update(userId) {
    set(updater(it.get(serializer)), serializer)
}

suspend inline fun <reified T : SettingsView<MatrixMessengerAccountSettings>> MatrixMessengerSettingsHolder.update(
    userId: UserId,
    noinline updater: (T) -> T,
) = update(userId, serializer(), updater)

suspend inline fun <reified T : SettingsView<MatrixMessengerSettings>> MatrixMessengerSettingsHolder.update(
    noinline updater: (T) -> T,
) = update(serializer(), updater)

expect fun platformMatrixMessengerSettingsHolderModule(): Module
