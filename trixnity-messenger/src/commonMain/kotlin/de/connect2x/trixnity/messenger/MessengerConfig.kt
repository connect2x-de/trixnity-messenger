package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.viewmodel.settings.PushMode


class MessengerConfig private constructor() {
    var appName: String = "Trixnity Messenger"
    var packageName: String = "de.connect2x"

    var encryptDb: Boolean = true

    // for privacy-first, override these values
    var defaultPushMode: PushMode = PushMode.PUSH
    var defaultPresenceIsPublic: Boolean = true
    var defaultReadMarkerIsPublic: Boolean = true
    var defaultTypingIsPublic: Boolean = true

    // notifications defaults
    var defaultNotificationPlaySound: Boolean = true
    var defaultNotificationShowPopup: Boolean = true
    var defaultNotificationShowText: Boolean = true

    var defaultHomeServer: String = ""

    var sendLogsEmailAddress: String? = null

    companion object { // no object since it would require @ThreadLocal for native
        val instance: MessengerConfig = MessengerConfig()
    }

    override fun toString(): String {
        return "MessengerConfig(appName='$appName', packageName='$packageName', encryptDb=$encryptDb, defaultPushMode=$defaultPushMode, defaultPresenceIsPublic=$defaultPresenceIsPublic, defaultReadMarkerIsPublic=$defaultReadMarkerIsPublic, defaultTypingIsPublic=$defaultTypingIsPublic, defaultNotificationPlaySound=$defaultNotificationPlaySound, defaultNotificationShowPopup=$defaultNotificationShowPopup, defaultNotificationShowText=$defaultNotificationShowText, defaultHomeServer='$defaultHomeServer', sendLogsEmailAddress=$sendLogsEmailAddress)"
    }

}
