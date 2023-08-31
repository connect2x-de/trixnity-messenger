package de.connect2x.trixnity.messenger.viewmodel.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import de.connect2x.trixnity.messenger.MessengerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class PushMode {
    @SerialName("none")
    NONE,

    @SerialName("polling")
    POLLING,

    @SerialName("push")
    PUSH,
}

private val log = KotlinLogging.logger { }

private const val PUSH_MODE = "pushMode"
private const val PRESENCE_IS_PUBLIC = "presenceIsPublic"
private const val READ_MARKER_IS_PUBLIC = "readMarkerIsPublic"
private const val TYPING_IS_PUBLIC = "typingIsPublic"
private const val NOTIFICATIONS_PLAY_SOUND = "notificationsPlaySound"
private const val NOTIFICATIONS_SHOW_POPUP = "notificationsShowPopup"
private const val NOTIFICATIONS_SHOW_TEXT = "notificationsShowText"
private const val ACTIVE_ACCOUNT = "activeAccount"
private const val PREFERRED_LANG = "preferredLang"
private const val URL_PROTOCOL = "urlProtocol"
private const val URL_HOST = "urlHost"
private const val SSO__REDIRECT_PATH = "ssoRedirectPath"

// FIXME global settings for account based settings
/**
 * Messenger settings for this device. Settings for accounts are determined in the following order (if `null`, the next
 * value is used): `settings value` -> `default value` (from [MessengerConfig])
 */
interface MessengerSettings {
    fun pushMode(accountName: String): PushMode
    fun setPushMode(accountName: String, newValue: PushMode?)
    fun pushModeFlow(accountName: String): Flow<PushMode>
    fun pushModeFlow(): Flow<Map<out String?, PushMode>>
    fun presenceIsPublic(accountName: String): Boolean
    fun setPresenceIsPublic(accountName: String, newValue: Boolean?)
    fun presenceIsPublicFlow(accountName: String): Flow<Boolean>
    fun readMarkerIsPublic(accountName: String): Boolean
    fun readMarkerIsPublicFlow(accountName: String): Flow<Boolean>
    fun setReadMarkerIsPublic(accountName: String, newValue: Boolean?)
    fun typingIsPublic(accountName: String): Boolean
    fun setTypingIsPublic(accountName: String, newValue: Boolean?)
    fun typingIsPublicFlow(accountName: String): Flow<Boolean>
    fun notificationsPlaySound(accountName: String): Boolean
    fun setNotificationsPlaySound(accountName: String, newValue: Boolean?)
    fun notificationsShowPopup(accountName: String): Boolean
    fun setNotificationsShowPopup(accountName: String, newValue: Boolean?)
    fun notificationsShowText(accountName: String): Boolean
    fun setNotificationsShowText(accountName: String, newValue: Boolean?)
    var activeAccount: String?
    var preferredLang: String?
    var urlProtocol: String
    var urlHost: String
    var ssoRedirectPath: String
}

@OptIn(ExperimentalSettingsApi::class)
class MessengerSettingsImpl(private val settings: Settings) : MessengerSettings {
    private val defaultPushMode: PushMode = MessengerConfig.instance.defaultPushMode
    private val defaultPresenceIsPublic: Boolean = MessengerConfig.instance.defaultPresenceIsPublic
    private val defaultReadMarkerIsPublic: Boolean = MessengerConfig.instance.defaultReadMarkerIsPublic
    private val defaultTypingIsPublic: Boolean = MessengerConfig.instance.defaultTypingIsPublic
    private val defaultNotificationPlaySound: Boolean = MessengerConfig.instance.defaultNotificationPlaySound
    private val defaultNotificationShowPopup: Boolean = MessengerConfig.instance.defaultNotificationShowPopup
    private val defaultNotificationShowText: Boolean = MessengerConfig.instance.defaultNotificationShowText

    private val json = Json

    override fun pushMode(accountName: String): PushMode =
        getValue(PUSH_MODE, accountName, defaultPushMode)

    override fun setPushMode(accountName: String, newValue: PushMode?) {
        setValue(PUSH_MODE, accountName, newValue, defaultPushMode)
    }

    override fun pushModeFlow(accountName: String): Flow<PushMode> =
        getFlowValue(PUSH_MODE, accountName, pushMode(accountName), defaultPushMode)

    override fun pushModeFlow(): Flow<Map<out String?, PushMode>> {
        return if (settings is ObservableSettings) {
            settings.getStringOrNullFlow(PUSH_MODE).map { jsonString ->
                jsonString?.let {
                    try {
                        json.decodeFromString<Map<String?, PushMode>>(jsonString).toMap().ifEmpty {
                            mapOf(null to defaultPushMode)
                        }
                    } catch (exc: Exception) {
                        mapOf(null to defaultPushMode)
                    }
                } ?: mapOf(null to defaultPushMode)
            }
        } else {
            log.warn { "You are trying to retrieve a Flow value from non-observable settings. This value will not change over time." }
            flowOf(mapOf(null to defaultPushMode))
        }
    }

    override fun presenceIsPublic(accountName: String): Boolean =
        getValue(PRESENCE_IS_PUBLIC, accountName, defaultPresenceIsPublic)

    override fun setPresenceIsPublic(accountName: String, newValue: Boolean?) =
        setValue(PRESENCE_IS_PUBLIC, accountName, newValue, defaultPresenceIsPublic)

    override fun presenceIsPublicFlow(accountName: String): Flow<Boolean> {
        return getFlowValue(PRESENCE_IS_PUBLIC, accountName, presenceIsPublic(accountName), defaultPresenceIsPublic)
    }

    override fun readMarkerIsPublic(accountName: String): Boolean =
        getValue(READ_MARKER_IS_PUBLIC, accountName, defaultReadMarkerIsPublic)

    override fun setReadMarkerIsPublic(accountName: String, newValue: Boolean?) =
        setValue(READ_MARKER_IS_PUBLIC, accountName, newValue, defaultReadMarkerIsPublic)

    override fun readMarkerIsPublicFlow(accountName: String): Flow<Boolean> =
        getFlowValue(READ_MARKER_IS_PUBLIC, accountName, readMarkerIsPublic(accountName), defaultReadMarkerIsPublic)

    override fun typingIsPublic(accountName: String): Boolean =
        getValue(TYPING_IS_PUBLIC, accountName, defaultTypingIsPublic)

    override fun setTypingIsPublic(accountName: String, newValue: Boolean?) {
        setValue(TYPING_IS_PUBLIC, accountName, newValue, defaultTypingIsPublic)
    }

    override fun typingIsPublicFlow(accountName: String): Flow<Boolean> =
        getFlowValue(TYPING_IS_PUBLIC, accountName, typingIsPublic(accountName), defaultTypingIsPublic)

    override fun notificationsPlaySound(accountName: String): Boolean =
        getValue(NOTIFICATIONS_PLAY_SOUND, accountName, defaultNotificationPlaySound)

    override fun setNotificationsPlaySound(accountName: String, newValue: Boolean?) =
        setValue(NOTIFICATIONS_PLAY_SOUND, accountName, newValue, defaultNotificationPlaySound)

    override fun notificationsShowPopup(accountName: String): Boolean =
        getValue(NOTIFICATIONS_SHOW_POPUP, accountName, defaultNotificationShowPopup)

    override fun setNotificationsShowPopup(accountName: String, newValue: Boolean?) =
        setValue(NOTIFICATIONS_SHOW_POPUP, accountName, newValue, defaultNotificationShowPopup)

    override fun notificationsShowText(accountName: String): Boolean =
        getValue(NOTIFICATIONS_SHOW_TEXT, accountName, defaultNotificationShowText)

    override fun setNotificationsShowText(accountName: String, newValue: Boolean?) =
        setValue(NOTIFICATIONS_SHOW_TEXT, accountName, newValue, defaultNotificationShowText)

    override var activeAccount: String?
        get() = settings.getStringOrNull(ACTIVE_ACCOUNT)
        set(value) = value?.let { settings.putString(ACTIVE_ACCOUNT, value) } ?: settings.remove(ACTIVE_ACCOUNT)
    override var preferredLang: String?
        get() = settings.getStringOrNull(PREFERRED_LANG)
        set(value) = value?.let { settings.putString(PREFERRED_LANG, value) } ?: settings.remove(PREFERRED_LANG)

    override var urlProtocol: String
        get() = settings.getStringOrNull(URL_PROTOCOL) ?: "trixnity"
        set(value) = value.let { settings.putString(URL_PROTOCOL, value) }

    override var urlHost: String
        get() = settings.getStringOrNull(URL_HOST) ?: "localhost"
        set(value) = value.let { settings.putString(URL_HOST, value) }

    override var ssoRedirectPath: String
        get() = settings.getStringOrNull(SSO__REDIRECT_PATH) ?: "sso"
        set(value) = value.let { settings.putString(SSO__REDIRECT_PATH, value) }

    private inline fun <reified T> getValue(key: String, accountName: String, defaultValue: T): T {
        log.trace { "get settings: `$key` of '$accountName' and default value: $defaultValue (settings (keys): ${settings.keys})" }
        val result = settings.getStringOrNull(key)?.let { jsonString ->
            try {
                val decodeFromString = json.decodeFromString<Map<String?, T>>(jsonString)
                log.trace { " $decodeFromString" }
                decodeFromString[accountName] ?: defaultValue
            } catch (exc: Exception) {
                defaultValue
            }
        } ?: defaultValue
        log.trace { "result: $result" }
        return result
    }

    private inline fun <reified T> getFlowValue(
        key: String,
        accountName: String,
        currentValue: T,
        defaultValue: T
    ): Flow<T> {
        return if (settings is ObservableSettings) {
            settings.getStringOrNullFlow(key).map {
                it?.let { jsonString ->
                    try {
                        json.decodeFromString<Map<String?, T>>(jsonString)[accountName] ?: defaultValue
                    } catch (exc: Exception) {
                        defaultValue
                    }
                } ?: defaultValue
            }
        } else {
            log.warn { "You are trying to retrieve a Flow value from non-observable settings. This value will not change over time." }
            flowOf(currentValue)
        }
    }

    private inline fun <reified T> setValue(key: String, accountName: String, value: T?, defaultValue: T) {
        log.debug { "set value of `$key` in account '$accountName' to $value" }
        val oldValue = settings.getStringOrNull(key)?.let { jsonString ->
            try {
                json.decodeFromString<Map<String?, T>>(jsonString)
            } catch (exc: Exception) {
                mapOf(accountName to defaultValue)
            }
        } ?: mapOf(accountName to defaultValue)
        val newValue: Map<String?, T> =
            if (value == null) {
                oldValue - accountName
            } else {
                oldValue - accountName + (accountName to value)
            }
        try {
            log.trace { "newValue: $newValue" }
            settings.putString(key, json.encodeToString(newValue))
        } catch (exc: Exception) {
            log.error(exc) { "cannot save value in settings" }
        }
    }
}

expect fun createSettings(): Settings
