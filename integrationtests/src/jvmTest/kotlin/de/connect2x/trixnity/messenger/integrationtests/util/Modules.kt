package de.connect2x.trixnity.messenger.integrationtests.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.PushMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.module

fun settingsModule() = module {
    single<MessengerSettings> {
        object : MessengerSettings {
            private val settings = HashMap<String, Any?>()

            override val defaultPushMode: PushMode = PushMode.PUSH
            override val defaultNotificationPlaySound: Boolean = false
            override val defaultNotificationShowPopup: Boolean = false
            override val defaultNotificationShowText: Boolean = false

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
            override var notificationPlaySound: Map<String?, Boolean>
                get() = settings["notificationPlaySound"]?.let { it as Map<String?, Boolean> }
                    ?: emptyMap()
                set(value) {
                    settings["notificationPlaySound"] = value
                }
            override var notificationsShowPopup: Map<String?, Boolean>
                get() = settings["notificationsShowPopup"]?.let { it as Map<String?, Boolean> }
                    ?: emptyMap()
                set(value) {
                    settings["notificationsShowPopup"] = value
                }
            override var notificationsShowText: Map<String?, Boolean>
                get() = settings["notificationsShowText"]?.let { it as Map<String?, Boolean> }
                    ?: emptyMap()
                set(value) {
                    settings["notificationsShowText"] = value
                }
            override var pushMode: Map<String?, PushMode>
                get() = settings["pushMode"]?.let { it as Map<String?, PushMode> } ?: emptyMap()
                set(value) {
                    settings["pushMode"] = value
                }
            override val pushModeFlow: Flow<Map<String?, PushMode>>
                get() = flowOf(settings["pushMode"]?.let { it as Map<String?, PushMode> } ?: emptyMap())

        }
    }
}