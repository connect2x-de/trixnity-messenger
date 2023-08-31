package de.connect2x.trixnity.messenger.viewmodel.util

import de.connect2x.trixnity.messenger.viewmodel.settings.MessengerSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.PushMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun testMessengerSettings(lang: String) = object : MessengerSettings {
    override fun pushMode(accountName: String): PushMode = PushMode.NONE
    override fun setPushMode(accountName: String, newValue: PushMode?) {}
    override fun pushModeFlow(accountName: String): Flow<PushMode> = flowOf(PushMode.NONE)
    override fun pushModeFlow(): Flow<Map<String?, PushMode>> = flowOf(mapOf(null to PushMode.NONE))
    override fun presenceIsPublic(accountName: String): Boolean = false
    override fun setPresenceIsPublic(accountName: String, newValue: Boolean?) {}
    override fun presenceIsPublicFlow(accountName: String): Flow<Boolean> = flowOf(false)
    override fun readMarkerIsPublic(accountName: String): Boolean = false
    override fun setReadMarkerIsPublic(accountName: String, newValue: Boolean?) {}
    override fun readMarkerIsPublicFlow(accountName: String): Flow<Boolean> = flowOf(false)
    override fun typingIsPublic(accountName: String): Boolean = false
    override fun setTypingIsPublic(accountName: String, newValue: Boolean?) {}
    override fun typingIsPublicFlow(accountName: String): Flow<Boolean> = flowOf(false)
    override fun notificationsPlaySound(accountName: String): Boolean = false
    override fun setNotificationsPlaySound(accountName: String, newValue: Boolean?) {}
    override fun notificationsShowPopup(accountName: String): Boolean = false
    override fun setNotificationsShowPopup(accountName: String, newValue: Boolean?) {}
    override fun notificationsShowText(accountName: String): Boolean = false
    override fun setNotificationsShowText(accountName: String, newValue: Boolean?) {}
    override var activeAccount: String?
        get() = null
        set(value) {}
    override var preferredLang: String?
        get() = lang
        set(value) {}
    override var urlProtocol: String = "trixnity"
    override var urlHost: String = "localhost"
    override var ssoRedirectPath: String = "sso"
}