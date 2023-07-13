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
private const val NOTIFICATIONS_PLAY_SOUND = "notificationsPlaySound"
private const val NOTIFICATIONS_SHOW_POPUP = "notificationsShowPopup"
private const val NOTIFICATIONS_SHOW_TEXT = "notificationsShowText"
private const val ACTIVE_ACCOUNT = "activeAccount"
private const val PREFERRED_LANG = "preferredLang"

/**
 * Messenger settings for this device. For settings that can be done individually for each account (`Map<String?, T>`),
 * `null` as key means we do not have any account information yet. This can be used to retrieve standard values.
 */
interface MessengerSettings {
    val defaultPushMode: PushMode
    val defaultNotificationPlaySound: Boolean
    val defaultNotificationShowPopup: Boolean
    val defaultNotificationShowText: Boolean

    var pushMode: Map<String?, PushMode>
    val pushModeFlow: Flow<Map<String?, PushMode>>
    var notificationPlaySound: Map<String?, Boolean>
    var notificationsShowPopup: Map<String?, Boolean>
    var notificationsShowText: Map<String?, Boolean>
    var activeAccount: String?
    var preferredLang: String?
}

@OptIn(ExperimentalSettingsApi::class)
class MessengerSettingsImpl : MessengerSettings {
    override val defaultPushMode: PushMode = MessengerConfig.instance.defaultPushMode
    override val defaultNotificationPlaySound: Boolean = true
    override val defaultNotificationShowPopup: Boolean = true
    override val defaultNotificationShowText: Boolean = true

    private val settings: Settings = createSettings()
    private val json = Json

    override var pushMode: Map<String?, PushMode>
        get() {
            return settings.getStringOrNull(PUSH_MODE)?.let { jsonString ->
                try {
                    json.decodeFromString<Map<String?, String>>(jsonString)
                        .map { (accountName, pushModeString) -> accountName to createPushMode(pushModeString) }.toMap()
                } catch (exc: Exception) {
                    mapOf(null to defaultPushMode)
                }
            } ?: mapOf(null to defaultPushMode)
        }
        set(value) {
            log.debug { "set push mode to: $value" }
            settings.putString(PUSH_MODE, json.encodeToString(value.map { it.key to it.value.name }.toMap()))
        }
    override val pushModeFlow: Flow<Map<String?, PushMode>> =
        if (settings is ObservableSettings) {
            settings.getStringOrNullFlow(PUSH_MODE).map { jsonString ->
                jsonString?.let {
                    try {
                        json.decodeFromString<Map<String?, String>>(jsonString)
                            .map { (accountName, pushModeString) -> accountName to createPushMode(pushModeString) }
                            .toMap()
                    } catch (exc: Exception) {
                        mapOf(null to defaultPushMode)
                    }
                } ?: mapOf(null to defaultPushMode)
            }
        } else {
            flowOf(pushMode)
        }

    override var notificationPlaySound: Map<String?, Boolean>
        get() = getValue(NOTIFICATIONS_PLAY_SOUND, defaultNotificationPlaySound)
        set(value) = setValue(value)
    override var notificationsShowPopup: Map<String?, Boolean>
        get() = getValue(NOTIFICATIONS_SHOW_POPUP, defaultNotificationShowPopup)
        set(value) = setValue(value)
    override var notificationsShowText: Map<String?, Boolean>
        get() = getValue(NOTIFICATIONS_SHOW_TEXT, defaultNotificationShowText)
        set(value) = setValue(value)
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

    private fun <T> getValue(key: String, defaultValue: T): Map<String?, T> =
        settings.getStringOrNull(key)?.let { jsonString ->
            try {
                json.decodeFromString<Map<String?, T>>(jsonString)
            } catch (exc: Exception) {
                mapOf(null to defaultValue)
            }
        } ?: mapOf(null to defaultValue)

    private fun <T> setValue(value: Map<String?, T>) {
        settings.putString(NOTIFICATIONS_PLAY_SOUND, json.encodeToString(value))
    }
}

expect fun createSettings(): Settings
