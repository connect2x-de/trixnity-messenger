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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class PushMode {
    NONE, POLLING, PUSH
}

private val log = KotlinLogging.logger { }

private const val PUSH_MODE = "pushMode"
private const val PRESENCE_IS_PUBLIC = "presenceIsPublic"
private const val READ_MARKER_IS_PUBLIC = "readMarkerIsPublic"
private const val NOTIFICATIONS_PLAY_SOUND = "notificationsPlaySound"
private const val NOTIFICATIONS_SHOW_POPUP = "notificationsShowPopup"
private const val NOTIFICATIONS_SHOW_TEXT = "notificationsShowText"
private const val ACTIVE_ACCOUNT = "activeAccount"
private const val PREFERRED_LANG = "preferredLang"

// FIXME global settings for account based settings
/**
 * Messenger settings for this device. Settings for accounts are determined in the following order (if `null`, the next
 * value is used): `settings value` -> `default value` (from [MessengerConfig])
 */
interface MessengerSettings {
    fun pushMode(accountName: String): PushMode
    fun setPushMode(accountName: String, newValue: PushMode)
    fun pushModeFlow(accountName: String): Flow<PushMode>
    fun presenceIsPublic(accountName: String): Boolean
    fun setPresenceIsPublic(accountName: String, newValue: Boolean)
    fun presenceIsPublicFlow(accountName: String): Flow<Boolean>
    fun readMarkerIsPublic(accountName: String): Boolean
    fun readMarkerIsPublicFlow(accountName: String): Flow<Boolean>
    fun setReadMarkerIsPublic(accountName: String, newValue: Boolean)
    fun notificationsPlaySound(accountName: String): Boolean
    fun setNotificationsPlaySound(accountName: String, newValue: Boolean)
    fun notificationsShowPopup(accountName: String): Boolean
    fun setNotificationsShowPopup(accountName: String, newValue: Boolean)
    fun notificationsShowText(accountName: String): Boolean
    fun setNotificationsShowText(accountName: String, newValue: Boolean)
    var activeAccount: String?
    var preferredLang: String?
}

@OptIn(ExperimentalSettingsApi::class)
class MessengerSettingsImpl : MessengerSettings {
    private val defaultPushMode: PushMode = MessengerConfig.instance.defaultPushMode
    private val defaultPresenceIsPublic: Boolean = MessengerConfig.instance.defaultPresenceIsPublic
    private val defaultReadMarkerIsPublic: Boolean = MessengerConfig.instance.defaultReadMarkerIsPublic
    private val defaultNotificationPlaySound: Boolean = MessengerConfig.instance.defaultNotificationPlaySound
    private val defaultNotificationShowPopup: Boolean = MessengerConfig.instance.defaultNotificationShowPopup
    private val defaultNotificationShowText: Boolean = MessengerConfig.instance.defaultNotificationShowText

    private val settings: Settings = createSettings()
    private val json = Json

    override fun pushMode(accountName: String): PushMode {
        return settings.getStringOrNull(PUSH_MODE)?.let { jsonString ->
            try {
                json.decodeFromString<Map<String, String>>(jsonString)
                    .map { (accountName, pushModeString) -> accountName to createPushMode(pushModeString) }
                    .toMap()[accountName]
                    ?: defaultPushMode
            } catch (exc: Exception) {
                defaultPushMode
            }
        } ?: defaultPushMode
    }

    override fun setPushMode(accountName: String, newValue: PushMode) {
        log.debug { "set push mode for account $accountName to: $newValue" }
        val oldMap = settings.getStringOrNull(PUSH_MODE)?.let { jsonString ->
            try {
                json.decodeFromString<Map<String, String>>(jsonString)
                    .map { (accountName, pushModeString) -> accountName to createPushMode(pushModeString) }
                    .toMap()
            } catch (exc: Exception) {
                mapOf()
            }
        } ?: mapOf()
        val newMap = oldMap - accountName + (accountName to newValue)
        settings.putString(PUSH_MODE, json.encodeToString(newMap))
    }

    override fun pushModeFlow(accountName: String): Flow<PushMode> {
        return if (settings is ObservableSettings) {
            settings.getStringOrNullFlow(PUSH_MODE).map { jsonString ->
                jsonString?.let {
                    try {
                        json.decodeFromString<Map<String, String>>(jsonString)
                            .map { (accountName, pushModeString) -> accountName to createPushMode(pushModeString) }
                            .toMap()[accountName]
                            ?: defaultPushMode
                    } catch (exc: Exception) {
                        defaultPushMode
                    }
                } ?: defaultPushMode
            }
        } else {
            log.warn { "You are trying to retrieve a Flow value from non-observable settings. This value will not change over time." }
            flowOf(pushMode(accountName))
        }
    }

    override fun presenceIsPublic(accountName: String): Boolean =
        getValue(PRESENCE_IS_PUBLIC, accountName, defaultPresenceIsPublic)

    override fun setPresenceIsPublic(accountName: String, newValue: Boolean) =
        setValue(PRESENCE_IS_PUBLIC, accountName, defaultPresenceIsPublic, newValue)

    override fun presenceIsPublicFlow(accountName: String): Flow<Boolean> {
        return getFlowValue(PRESENCE_IS_PUBLIC, accountName, defaultPresenceIsPublic, presenceIsPublic(accountName))
    }

    override fun readMarkerIsPublic(accountName: String): Boolean =
        getValue(READ_MARKER_IS_PUBLIC, accountName, defaultReadMarkerIsPublic)

    override fun setReadMarkerIsPublic(accountName: String, newValue: Boolean) =
        setValue(READ_MARKER_IS_PUBLIC, accountName, defaultReadMarkerIsPublic, newValue)

    override fun readMarkerIsPublicFlow(accountName: String): Flow<Boolean> =
        getFlowValue(READ_MARKER_IS_PUBLIC, accountName, defaultReadMarkerIsPublic, readMarkerIsPublic(accountName))

    override fun notificationsPlaySound(accountName: String): Boolean =
        getValue(NOTIFICATIONS_PLAY_SOUND, accountName, defaultNotificationPlaySound)

    override fun setNotificationsPlaySound(accountName: String, newValue: Boolean) =
        setValue(NOTIFICATIONS_PLAY_SOUND, accountName, defaultNotificationPlaySound, newValue)

    override fun notificationsShowPopup(accountName: String): Boolean =
        getValue(NOTIFICATIONS_SHOW_POPUP, accountName, defaultNotificationShowPopup)

    override fun setNotificationsShowPopup(accountName: String, newValue: Boolean) =
        setValue(NOTIFICATIONS_SHOW_POPUP, accountName, defaultNotificationShowPopup, newValue)

    override fun notificationsShowText(accountName: String): Boolean =
        getValue(NOTIFICATIONS_SHOW_TEXT, accountName, defaultNotificationShowText)

    override fun setNotificationsShowText(accountName: String, newValue: Boolean) =
        setValue(NOTIFICATIONS_SHOW_TEXT, accountName, defaultNotificationShowText, newValue)

    override var activeAccount: String?
        get() = settings.getStringOrNull(ACTIVE_ACCOUNT)
        set(value) = value?.let { settings.putString(ACTIVE_ACCOUNT, value) } ?: settings.remove(ACTIVE_ACCOUNT)
    override var preferredLang: String?
        get() = settings.getStringOrNull(PREFERRED_LANG)
        set(value) = value?.let { settings.putString(PREFERRED_LANG, value) } ?: settings.remove(PREFERRED_LANG)

    private fun createPushMode(pushModeString: String) = try {
        log.debug { "create pushMode '$pushModeString'" }
        PushMode.valueOf(pushModeString)
    } catch (exc: IllegalArgumentException) {
        defaultPushMode
    }

    private fun <T> getValue(key: String, accountName: String, defaultValue: T): T =
        settings.getStringOrNull(key)?.let { jsonString ->
            try {
                json.decodeFromString<Map<String, T>>(jsonString)[accountName] ?: defaultValue
            } catch (exc: Exception) {
                defaultValue
            }
        } ?: defaultValue

    private fun <T> getFlowValue(key: String, accountName: String, defaultValue: T, currentValue: T): Flow<T> {
        return if (settings is ObservableSettings) {
            settings.getStringOrNullFlow(key).map {
                it?.let { jsonString ->
                    try {
                        json.decodeFromString<Map<String, T>>(jsonString)[accountName] ?: defaultValue
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

    private fun <T> setValue(key: String, accountName: String, defaultValue: T, value: T) {
        log.debug { "set value of `$key` in account '$accountName' to $value" }
        val oldValue = settings.getStringOrNull(key)?.let { jsonString ->
            try {
                json.decodeFromString<Map<String, T>>(jsonString)
            } catch (exc: Exception) {
                mapOf(accountName to defaultValue)
            }
        } ?: mapOf(accountName to defaultValue)
        val newValue = oldValue - accountName + (accountName to value)
        settings.putString(key, json.encodeToString(newValue))
    }
}

expect fun createSettings(): Settings
