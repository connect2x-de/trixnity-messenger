package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.PushMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun defaultMessengerSettings(lang: String) = object : MessengerSettings {
    override val defaultPushMode: PushMode = PushMode.NONE
    override val defaultNotificationPlaySound: Boolean = false
    override val defaultNotificationShowPopup: Boolean = false
    override val defaultNotificationShowText: Boolean = false
    override var pushMode: Map<String?, PushMode>
        get() = emptyMap()
        set(value) {}
    override val pushModeFlow: Flow<Map<String?, PushMode>>
        get() = flowOf(emptyMap())
    override var notificationPlaySound: Map<String?, Boolean>
        get() = emptyMap()
        set(value) {}
    override var notificationsShowPopup: Map<String?, Boolean>
        get() = emptyMap()
        set(value) {}
    override var notificationsShowText: Map<String?, Boolean>
        get() = emptyMap()
        set(value) {}
    override var accountNames: List<String>
        get() = emptyList()
        set(value) {}
    override var activeAccount: String?
        get() = null
        set(value) {}
    override var preferredLang: String?
        get() = lang
        set(value) {}

}