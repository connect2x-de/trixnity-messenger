package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.PushMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

fun settingsModule() = module {
    single<MessengerSettings> {
        object : MessengerSettings {
            val defaultPushMode: PushMode = PushMode.PUSH
            val defaultPresenceIsPublic: Boolean = false
            val defaultReadMarkerIsPublic: Boolean = false
            val defaultNotificationPlaySound: Boolean = false
            val defaultNotificationShowPopup: Boolean = false
            val defaultNotificationShowText: Boolean = false

            private val settings = HashMap<String, Any?>()

            override fun pushMode(accountName: String): PushMode =
                getPushMode()[accountName] ?: defaultPushMode

            override fun setPushMode(accountName: String, newValue: PushMode?) {
                settings["pushMode"] = setNewValue(getPushMode(), accountName, newValue)
            }

            override fun pushModeFlow(): Flow<Map<String?, PushMode>> = flowOf(getPushMode())

            override fun pushModeFlow(accountName: String): Flow<PushMode> =
                pushModeFlow().map { it[accountName] ?: defaultPushMode }

            override fun presenceIsPublic(accountName: String): Boolean =
                getPresenceIsPublic()[accountName] ?: defaultPresenceIsPublic

            override fun setPresenceIsPublic(accountName: String, newValue: Boolean?) {
                settings["presenceIsPublic"] = setNewValue(getPresenceIsPublic(), accountName, newValue)
            }

            override fun presenceIsPublicFlow(accountName: String): Flow<Boolean> =
                flowOf(presenceIsPublic(accountName))

            override fun readMarkerIsPublic(accountName: String): Boolean =
                getPresenceIsPublic()[accountName] ?: defaultReadMarkerIsPublic

            override fun setReadMarkerIsPublic(accountName: String, newValue: Boolean?) {
                settings["readMarkerIsPublic"] = setNewValue(getReadMarkerIsPublic(), accountName, newValue)
            }

            override fun readMarkerIsPublicFlow(accountName: String): Flow<Boolean> =
                flowOf(readMarkerIsPublic(accountName))

            override fun notificationsPlaySound(accountName: String): Boolean =
                getNotificationsPlaySound()[accountName] ?: defaultNotificationPlaySound

            override fun setNotificationsPlaySound(accountName: String, newValue: Boolean?) {
                settings["notificationsPlaySound"] = setNewValue(getNotificationsPlaySound(), accountName, newValue)
            }

            override fun notificationsShowPopup(accountName: String): Boolean =
                getNotificationsShowPopup()[accountName] ?: defaultNotificationShowPopup

            override fun setNotificationsShowPopup(accountName: String, newValue: Boolean?) {
                settings["notificationsShowPopup"] = setNewValue(getNotificationsShowPopup(), accountName, newValue)
            }

            override fun notificationsShowText(accountName: String): Boolean =
                getNotificationsShowText()[accountName] ?: defaultNotificationShowText

            override fun setNotificationsShowText(accountName: String, newValue: Boolean?) {
                settings["notificationsShowText"] = setNewValue(getNotificationsShowText(), accountName, newValue)
            }

            private fun getPushMode(): Map<String?, PushMode> =
                (settings["pushMode"] as Map<String?, PushMode>?) ?: emptyMap()


            private fun getPresenceIsPublic(): Map<String?, Boolean> =
                (settings["presenceIsPublic"] as Map<String?, Boolean>?) ?: emptyMap()

            private fun getReadMarkerIsPublic(): Map<String?, Boolean> =
                (settings["readMarkerIsPublic"] as Map<String?, Boolean>?) ?: emptyMap()

            private fun getNotificationsPlaySound(): Map<String?, Boolean> =
                (settings["notificationsPlaySound"] as Map<String?, Boolean>?) ?: emptyMap()

            private fun getNotificationsShowPopup(): Map<String?, Boolean> =
                (settings["notificationsShowPopup"] as Map<String?, Boolean>?) ?: emptyMap()

            private fun getNotificationsShowText(): Map<String?, Boolean> =
                (settings["notificationsShowText"] as Map<String?, Boolean>?) ?: emptyMap()

            private fun <T> setNewValue(
                getMap: Map<String?, T>,
                accountName: String,
                newValue: T?
            ): Map<String?, T> =
                if (newValue == null) {
                    getMap - accountName
                } else {
                    getMap - accountName + (accountName to newValue)
                }

            override var activeAccount: String?
                get() = settings["activeAccount"]?.let { it as String? }
                set(value) {
                    settings["activeAccount"] = value
                }
            override var preferredLang: String?
                get() = settings["preferredLang"]?.let { it as String? }
                set(value) {
                    settings["preferredLang"] = value
                }
        }
    }
}