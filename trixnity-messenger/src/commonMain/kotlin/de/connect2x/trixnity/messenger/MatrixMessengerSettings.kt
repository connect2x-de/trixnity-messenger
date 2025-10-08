@file:Suppress("DEPRECATION") // because of LegacySecretByteArrayKey TODO: remove this in the future

package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.secrets.SecretByteArray
import de.connect2x.trixnity.messenger.secrets.SecretByteArrayKeyInfo
import de.connect2x.trixnity.messenger.settings.JsonDelegateSerializer
import de.connect2x.trixnity.messenger.settings.MutableSettings
import de.connect2x.trixnity.messenger.settings.MutableSettingsImpl
import de.connect2x.trixnity.messenger.settings.NestedSettingsView
import de.connect2x.trixnity.messenger.settings.SettingsHolder
import de.connect2x.trixnity.messenger.settings.SettingsHolderImpl
import de.connect2x.trixnity.messenger.settings.SettingsImpl
import de.connect2x.trixnity.messenger.settings.SettingsStorage
import de.connect2x.trixnity.messenger.settings.SettingsView
import de.connect2x.trixnity.messenger.settings.get
import de.connect2x.trixnity.messenger.settings.set
import de.connect2x.trixnity.messenger.settings.update
import de.connect2x.trixnity.messenger.util.ByteArrayBase64Serializer
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

private val log = KotlinLogging.logger { }

@Serializable
data class MatrixMessengerSettingsBase(
    val accounts: Map<UserId, MatrixMessengerAccountSettings> = mapOf(),
    val preferredLang: String? = null,
    val selectedAccount: UserId? = null, // TODO should be saved via decompose state preservation
    val ssoState: SSOState? = null,

    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    /**
     * The high contrast mode is an a11y option to have fewer colors and more contrast on all UI elements to improve
     * readability.
     */
    val isHighContrast: Boolean = false,
    /**
     * Focused elements are highlighted more to enable better keyboard navigation.
     */
    val isFocusHighlighting: Boolean = false,
    val accentColor: Long? = null,
    val fontSize: Float? = null,
    val displaySize: Float? = null,
    val applySystemSizes: Boolean = true,
) : SettingsView<MatrixMessengerSettings>

@Serializable
@NestedSettingsView("secretByteArrays")
data class SecretByteArraySettings(
    val secrets: Map<String, SecretByteArray>? = null,
    val keyInfo: Map<String, SecretByteArrayKeyInfo>? = null,
    val mac: @Serializable(with = ByteArrayBase64Serializer::class) ByteArray? = null,
) : SettingsView<MatrixMessengerSettings> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SecretByteArraySettings

        if (secrets != other.secrets) return false
        if (keyInfo != other.keyInfo) return false
        if (!mac.contentEquals(other.mac)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = secrets.hashCode()
        result = 31 * result + keyInfo.hashCode()
        result = 31 * result + mac.contentHashCode()
        return result
    }
}

@Serializable
data class MatrixMessengerAccountSettingsBase(
    val displayName: String? = null,
    val displayColor: Long? = null,
    val presenceIsPublic: Boolean = true,
    val readMarkerIsPublic: Boolean = true,
    val typingIsPublic: Boolean = true,
    val accountSetupFinished: Boolean = false,
) : SettingsView<MatrixMessengerAccountSettings> {
    companion object {
        fun withConfigDefaults(
            displayColor: Long?,
            config: MatrixMessengerConfiguration,
        ) = MatrixMessengerAccountSettingsBase(
            displayColor = displayColor,
            presenceIsPublic = config.defaultPresenceIsPublic,
            readMarkerIsPublic = config.defaultReadMarkerIsPublic,
            typingIsPublic = config.defaultTypingIsPublic,
            accountSetupFinished = config.useAccountSetupWizard.not(),
        )
    }
}

@Serializable
@NestedSettingsView("notification")
data class MatrixMessengerAccountNotificationSettings(
    val playSound: Boolean = true,
    val showDetails: Boolean = true,
) : SettingsView<MatrixMessengerAccountSettings>

data class MatrixMessengerSettings(
    private val delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMessengerSettings>(delegate) {
    val base by lazy { get<MatrixMessengerSettings, MatrixMessengerSettingsBase>() }
    val secretByteArrays by lazy { get<MatrixMessengerSettings, SecretByteArraySettings>() }
}

@Serializable(MatrixMessengerAccountSettingsSerializer::class)
data class MatrixMessengerAccountSettings(
    private val delegate: Map<String, JsonElement>
) : SettingsImpl<MatrixMessengerAccountSettings>(delegate) {
    val base by lazy { get<MatrixMessengerAccountSettings, MatrixMessengerAccountSettingsBase>() }
    val notification by lazy { get<MatrixMessengerAccountSettings, MatrixMessengerAccountNotificationSettings>() }
}

internal object MatrixMessengerAccountSettingsSerializer : JsonDelegateSerializer<MatrixMessengerAccountSettings>(
    "MatrixMessengerAccountSettingsSerializer", ::MatrixMessengerAccountSettings
)

interface MatrixMessengerSettingsHolder : SettingsHolder<MatrixMessengerSettings> {
    operator fun get(userId: UserId): Flow<MatrixMessengerAccountSettings?>

    suspend fun create(
        userId: UserId,
        settings: MatrixMessengerAccountSettingsBase
    )

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

    override suspend fun create(
        userId: UserId,
        settings: MatrixMessengerAccountSettingsBase,
    ) = update<MatrixMessengerSettingsBase> {
        log.debug { "create account settings for $userId" }
        val accountSettings = MutableSettingsImpl(MatrixMessengerAccountSettings(emptyMap()))
        accountSettings.set(settings)
        it.copy(accounts = it.accounts + (userId to MatrixMessengerAccountSettings(accountSettings)))
    }

    override suspend fun update(
        userId: UserId,
        updater: MutableSettings<MatrixMessengerAccountSettings>.(MatrixMessengerAccountSettings) -> Unit,
    ) = update<MatrixMessengerSettingsBase> {
        log.debug { "update account settings for $userId" }
        val oldAccounts = it.accounts
        val oldAccountSettings = oldAccounts[userId] ?: return@update it
        val newAccountSettings = MutableSettingsImpl(oldAccountSettings)
        with(newAccountSettings) {
            updater(oldAccountSettings)
        }
        it.copy(accounts = oldAccounts + (userId to MatrixMessengerAccountSettings(newAccountSettings)))
    }

    override suspend fun delete(userId: UserId) = update<MatrixMessengerSettingsBase> {
        log.debug { "delete account settings for $userId" }
        val accounts = it.accounts - userId
        val selectedAccount =
            if (it.selectedAccount == userId) accounts.keys.firstOrNull()
            else it.selectedAccount
        it.copy(
            accounts = accounts,
            selectedAccount = selectedAccount,
        )
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
